package com.sequenceiq.cloudbreak.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Controller
public class CredentialController {

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @RequestMapping(value = "user/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> savePrivateCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest) {
        MDCBuilder.buildMdcContext(user);
        return createCredential(user, credentialRequest, false);
    }

    @RequestMapping(value = "account/credentials", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> saveAccountCredential(@ModelAttribute("user") CbUser user, @Valid @RequestBody CredentialJson credentialRequest) {
        MDCBuilder.buildMdcContext(user);
        return createCredential(user, credentialRequest, true);
    }

    @RequestMapping(value = "user/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getPrivateCredentials(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Credential> credentials = credentialService.retrievePrivateCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<CredentialJson>> getAccountCredentials(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Credential> credentials = credentialService.retrieveAccountCredentials(user);
        return new ResponseEntity<>(convertCredentials(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getPrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Credential credentials = credentialService.getPrivateCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getAccountCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Credential credentials = credentialService.getPublicCredential(name, user);
        return new ResponseEntity<>(convert(credentials), HttpStatus.OK);
    }

    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<CredentialJson> getCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        MDCBuilder.buildMdcContext(user);
        Credential credential = credentialService.get(credentialId);
        return new ResponseEntity<>(convert(credential), HttpStatus.OK);
    }

    @RequestMapping(value = "credentials/{credentialId}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deleteCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId) {
        MDCBuilder.buildMdcContext(user);
        credentialService.delete(credentialId, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "account/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePublicCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "user/credentials/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<CredentialJson> deletePrivateCredential(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        credentialService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "credentials/certificate/{credentialId}", method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView getJksFile(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        MDCBuilder.buildMdcContext(user);
        AzureCredential credential = (AzureCredential) credentialService.get(credentialId);
        File cerFile = azureStackUtil.buildAzureCerFile(credential);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + user.getUsername() + ".cer");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }

    @RequestMapping(value = "credentials/certificate/{credentialId}", method = RequestMethod.PUT)
    @ResponseBody
    public ModelAndView refreshCredential(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        MDCBuilder.buildMdcContext(user);
        Credential credential = credentialService.update(credentialId);
        if (credential instanceof AzureCredential) {
            File cerFile = azureStackUtil.buildAzureCerFile((AzureCredential) credential);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + user.getUsername() + ".cer");
            FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        }
        return null;
    }

    @RequestMapping(value = "credentials/{credentialId}/sshkey", method = RequestMethod.GET)
    @ResponseBody
    public ModelAndView getSshFile(@ModelAttribute("user") CbUser user, @PathVariable Long credentialId, HttpServletResponse response) throws Exception {
        MDCBuilder.buildMdcContext(user);
        AzureCredential credential = (AzureCredential) credentialService.get(credentialId);
        File cerFile = azureStackUtil.buildAzureSshCerFile(credential);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=public_key.pem");
        FileCopyUtils.copy(Files.readAllBytes(cerFile.toPath()), response.getOutputStream());
        return null;
    }

    private ResponseEntity<IdJson> createCredential(CbUser user, CredentialJson credentialRequest, boolean publicInAccount) {
        Credential credential = convert(credentialRequest, publicInAccount);
        credential = credentialService.create(user, credential);
        return new ResponseEntity<>(new IdJson(credential.getId()), HttpStatus.CREATED);
    }

    private Credential convert(CredentialJson json, boolean publicInAccount) {
        Credential converted = null;
        switch (json.getCloudPlatform()) {
        case AWS:
            converted = conversionService.convert(json, AwsCredential.class);
            break;
        case AZURE:
            converted = conversionService.convert(json, AzureCredential.class);
            break;
        case GCC:
            converted = conversionService.convert(json, GccCredential.class);
            break;
        case OPENSTACK:
            converted = conversionService.convert(json, OpenStackCredential.class);
            break;
        default:
            throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", json.getCloudPlatform()));
        }
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private CredentialJson convert(Credential credential) {

        switch (credential.cloudPlatform()) {
        case AWS:
            return conversionService.convert((AwsCredential) credential, CredentialJson.class);
        case AZURE:
            return conversionService.convert((AzureCredential) credential, CredentialJson.class);
        case GCC:
            return conversionService.convert((GccCredential) credential, CredentialJson.class);
        case OPENSTACK:
            return conversionService.convert((OpenStackCredential) credential, CredentialJson.class);
        default:
            throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", credential.cloudPlatform()));
        }
    }

    private Set<CredentialJson> convertCredentials(Set<Credential> credentials) {
        Set<CredentialJson> jsonSet = new HashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convert(credential));
        }
        return jsonSet;
    }
}
