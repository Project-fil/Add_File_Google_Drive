package com.example.testgoogledrivespring.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.api.services.drive.Drive;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import static com.example.testgoogledrivespring.controller.SpringHomeController.*;

@RestController
public class FileController {

    @Value("${google.secret.key.path}")
    private Resource gdSecretKeys;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;

    private GoogleAuthorizationCodeFlow flow;

    private Credential cred;

    private Drive drive;

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKeys.getInputStream()));
        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile()))
                .build();
    }

    @PostMapping(value = "/create/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createFileOnGoogleDrive(HttpServletResponse response, @RequestParam("multipartFile") MultipartFile multipartFile) throws IOException {
        cred = flow.loadCredential(USER_IDENTIFIER_KEY);
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();
        File file = new File();
        file.setName(Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(0, multipartFile.getOriginalFilename().lastIndexOf(".")));
        AbstractInputStreamContent content = new ByteArrayContent(multipartFile.getContentType(), multipartFile.getBytes());
        File uploadFile = drive.files().create(file, content).setFields("id, webContentLink, webViewLink, parents").execute();
        String fileReference = String.format("{fileID: %s }", uploadFile.getId());
        response.getWriter().write(fileReference);
    }

}
