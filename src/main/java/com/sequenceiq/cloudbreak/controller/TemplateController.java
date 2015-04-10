package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UnknownFormatConversionException;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Controller
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @RequestMapping(value = "user/templates", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createPrivateTemplate(@ModelAttribute("user") CbUser user, @RequestBody @Valid TemplateJson templateRequest) {
        MDCBuilder.buildMdcContext(user);
        return createTemplate(user, templateRequest, false);
    }

    @RequestMapping(value = "account/templates", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<IdJson> createAccountTemplate(@ModelAttribute("user") CbUser user, @RequestBody @Valid TemplateJson templateRequest) {
        MDCBuilder.buildMdcContext(user);
        return createTemplate(user, templateRequest, true);
    }

    @RequestMapping(value = "user/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getPrivateTemplates(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Template> templates = templateService.retrievePrivateTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @RequestMapping(value = "account/templates", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Set<TemplateJson>> getAccountTemplates(@ModelAttribute("user") CbUser user) {
        MDCBuilder.buildMdcContext(user);
        Set<Template> templates = templateService.retrieveAccountTemplates(user);
        return new ResponseEntity<>(convert(templates), HttpStatus.OK);
    }

    @RequestMapping(value = "templates/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        Template template = templateService.get(id);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @RequestMapping(value = "user/templates/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplateInPrivate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Template template = templateService.getPrivateTemplate(name, user);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @RequestMapping(value = "account/templates/{name}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TemplateJson> getTemplateInAccount(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        Template template = templateService.getPublicTemplate(name, user);
        TemplateJson templateJson = convert(template);
        return new ResponseEntity<>(templateJson, HttpStatus.OK);
    }

    @RequestMapping(value = "templates/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deleteTemplate(@ModelAttribute("user") CbUser user, @PathVariable Long id) {
        MDCBuilder.buildMdcContext(user);
        templateService.delete(id, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "account/templates/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePublicTemplate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        templateService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = "user/templates/{name}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<TemplateJson> deletePrivateTemplate(@ModelAttribute("user") CbUser user, @PathVariable String name) {
        MDCBuilder.buildMdcContext(user);
        templateService.delete(name, user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private ResponseEntity<IdJson> createTemplate(CbUser user, TemplateJson templateRequest, boolean publicInAccount) {
        Template template = convert(templateRequest, publicInAccount);
        template = templateService.create(user, template);
        return new ResponseEntity<>(new IdJson(template.getId()), HttpStatus.CREATED);
    }

    private Template convert(TemplateJson templateRequest, boolean publicInAccount) {
        Template converted = null;
        switch (templateRequest.getCloudPlatform()) {
        case AWS:
            converted = conversionService.convert(templateRequest, AwsTemplate.class);
            break;
        case AZURE:
            converted = conversionService.convert(templateRequest, AzureTemplate.class);
            break;
        case GCC:
            converted = conversionService.convert(templateRequest, GccTemplate.class);
            break;
        case OPENSTACK:
            converted = conversionService.convert(templateRequest, OpenStackTemplate.class);
            break;
        default:
            throw new UnknownFormatConversionException(String.format("The cloudPlatform '%s' is not supported.", templateRequest.getCloudPlatform()));
        }
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private TemplateJson convert(Template template) {
        return conversionService.convert(template, TemplateJson.class);
    }

    private Set<TemplateJson> convert(Set<Template> templates) {
        Set<TemplateJson> jsons = new HashSet<>();
        for (Template template : templates) {
            jsons.add(convert(template));
        }
        return jsons;
    }
}
