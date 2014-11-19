package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class AzureStackUtil {

    public static final int NOT_FOUND = 404;
    public static final String NAME = "name";
    public static final String SERVICENAME = "serviceName";
    public static final String ERROR = "\"error\":\"Could not fetch data from azure\"";
    public static final String CREDENTIAL = "credential";
    public static final String EMAILASFOLDER = "emailAsFolder";
    public static final String IMAGE_NAME = "ambari-docker-v1";
    public static final Logger LOGGER = LoggerFactory.getLogger(AzureStackUtil.class);

    @Value("${cb.azure.image.uri}")
    private String baseImageUri;

    @Autowired
    private KeyGeneratorService keyGeneratorService;

    public String getOsImageName(Credential credential) {
        String[] split = baseImageUri.split("/");
        AzureCredential azureCredential = (AzureCredential) credential;
        return String.format("%s-%s-%s", azureCredential.getCommonName(), IMAGE_NAME,
                split[split.length - 1].replaceAll(".vhd", ""));
    }

    public static AzureClient createAzureClient(AzureCredential credential) {
        MDCBuilder.buildMdcContext(credential);
        try {
            String jksPath = credential.getId() == null ? "/tmp/" + new Date().getTime() + ".jks" : "/tmp/" + credential.getId() + ".jks";
            File jksFile = new File(jksPath);
            if (!jksFile.exists()) {
                FileOutputStream output = new FileOutputStream(jksFile);
                IOUtils.write(Base64.decodeBase64(credential.getJksFile()), output);
            }
            return new AzureClient(credential.getSubscriptionId(), jksFile.getAbsolutePath(), credential.getJks());
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new InternalServerException(e.getMessage());
        }
    }

    public static Map<String, String> createVMContext(String vmName) {
        Map<String, String> context = new HashMap<>();
        context.put(SERVICENAME, vmName);
        context.put(NAME, vmName);
        return context;
    }

    public File buildAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh.cer" : "/tmp/" + azureCredential.getId() + "ssh.cer";
        File sshCerfile = new File(sshCerPath);
        if (!sshCerfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerfile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getSshCerFile()), output);
        }
        return sshCerfile;
    }

    public File buildAzureCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshCerPath = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + ".cer" : "/tmp/" + azureCredential.getId() + ".cer";
        File sshCerfile = new File(sshCerPath);
        if (!sshCerfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshCerfile);
            IOUtils.write(Base64.decodeBase64(azureCredential.getCerFile()), output);
        }
        return sshCerfile;
    }

    public AzureCredential generateAzureServiceFiles(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String serviceFilesPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() : "/tmp/" + azureCredential.getId();
        try {
            keyGeneratorService.generateKey("cloudbreak", azureCredential, "mydomain", serviceFilesPathWithoutExtension + ".jks");
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = azureCredential.getJks().toCharArray();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(serviceFilesPathWithoutExtension + ".jks"));
                ks.load(fis, pass);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            Certificate certificate = ks.getCertificate("mydomain");
            final FileOutputStream os = new FileOutputStream(serviceFilesPathWithoutExtension + ".cer");
            os.write(Base64.encodeBase64(certificate.getEncoded(), true));
            os.close();
            azureCredential.setCerFile(FileReaderUtils.readFileFromPath(serviceFilesPathWithoutExtension + ".cer"));
            azureCredential.setJksFile(FileReaderUtils.readBinaryFileFromPath(serviceFilesPathWithoutExtension + ".jks"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new InternalServerException("There was a problem with the certificate generation", e);
        }
        return azureCredential;
    }

    public AzureCredential generateAzureSshCerFile(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        String sshPemPathWithoutExtension = azureCredential.getId() == null ? "/tmp/" + new Date().getTime() + "ssh" : "/tmp/" + azureCredential.getId() + "ssh";
        String sshPemPath = sshPemPathWithoutExtension + ".pem";
        File sshPemfile = new File(sshPemPath);
        if (!sshPemfile.exists()) {
            FileOutputStream output = new FileOutputStream(sshPemfile);
            IOUtils.write(azureCredential.getPublicKey(), output);
        }
        try {
            keyGeneratorService.generateSshKey(sshPemPathWithoutExtension);
            azureCredential.setSshCerFile(FileReaderUtils.readFileFromPath(sshPemPathWithoutExtension + ".cer"));
        } catch (InterruptedException e) {
            LOGGER.error("An error occured under the ssh generation for {} template. The error was: {} {}", azureCredential.getId(), e.getMessage(), e);
            throw new InternalServerException(e.getMessage());
        }

        return azureCredential;
    }

    public X509Certificate createX509Certificate(AzureCredential azureCredential) throws IOException, GeneralSecurityException {
        return new X509Certificate(buildAzureSshCerFile(azureCredential).getAbsolutePath());
    }

}
