package com.example.testgoogledrivespring.controller;

import com.example.testgoogledrivespring.exception.EntityNotFoundException;
import com.example.testgoogledrivespring.exception.NotValidException;
import com.example.testgoogledrivespring.payload.response.ImageResponse;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
public class SpringHomeController {

    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    public static final String APPLICATION_NAME = "googleDriveSpringBoot";

    public static final String USER_IDENTIFIER_KEY = "MY_DUMMY_USER";

    private static final Map<String, String> imageIdAndName = new HashMap<>();

    private static Drive drive;

    @Value("${google.oauth.callback.uri}")
    private String CALLBACK_URI;

    @Value("${google.secret.key.path}")
    private Resource gdSecretKeys;

    @Value("${google.credentials.folder.path}")
    private Resource credentialsFolder;

    private GoogleAuthorizationCodeFlow flow;

    @PostConstruct
    public void init() throws IOException {
        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(gdSecretKeys.getInputStream()));
        flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(credentialsFolder.getFile()))
                .build();
    }

    @GetMapping("/")
    public void doGoogleSignIn(HttpServletResponse response) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        String redirectURL = url.setRedirectUri(CALLBACK_URI).setAccessType("offline").build();
        response.sendRedirect(redirectURL);
    }

    @GetMapping("/oauth")
    public void saveAuthorizationCode(HttpServletRequest request) throws IOException {
        String code = request.getParameter("code");
        if (code != null) {
            saveToken(code);
        } else {
            throw new NotValidException("token not valid");
        }
    }

    private void saveToken(String code) throws IOException {
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(CALLBACK_URI).execute();
        flow.createAndStoreCredential(response, USER_IDENTIFIER_KEY);
    }

    @GetMapping(value = {"/get/{imageName}/or/{imageIdFromParam}"}, produces = {"application/json"})
    public ResponseEntity<Object> getImageByNameOrId(@PathVariable String imageName, @PathVariable String imageIdFromParam) throws IOException, EntityNotFoundException {
        Credential cred = flow.loadCredential(USER_IDENTIFIER_KEY);
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();

        System.out.println(imageIdAndName);
        ImageResponse imageResponse = null;
        String imageId = null;
        if (imageIdAndName.containsValue(imageName)) {
            for (Map.Entry<String, String> item : imageIdAndName.entrySet()) {
                if (item.getValue().equals(imageName)) {
                    imageId = item.getKey();
                }
            }
        } else {
            imageId = imageIdFromParam;
        }
            FileList fileList = drive.files().list().setFields("files(id, name, webContentLink, webViewLink)").execute();

            for (File file : fileList.getFiles()) {
                if (file.getId().equals(imageId)) {
                    imageResponse = new ImageResponse();
                    imageResponse.setId(file.getId());
                    imageResponse.setName(file.getName());
                    imageResponse.setGetWebContentLink(file.getWebContentLink());
                    imageResponse.setWebViewLink(file.getWebViewLink());
                    break;
                }
            }
        if (Objects.nonNull(imageResponse)) {
            return ResponseEntity.ok(imageResponse);
        } else {
            return ResponseEntity.status(404).body("Entity not found");
        }
    }

    @GetMapping(value = {"/get/{imageId}"}, produces = {"application/json"})
    public ResponseEntity<Object> getImageById(@PathVariable String imageId) throws IOException, EntityNotFoundException {
        Credential cred = flow.loadCredential(USER_IDENTIFIER_KEY);
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();

        ImageResponse imageResponse = null;

        FileList fileList = drive.files().list().setFields("files(id, name, webContentLink, webViewLink, imageMediaMetadata)").execute();

        for (File file : fileList.getFiles()) {
            if (file.getId().equals(imageId)) {
                imageResponse = new ImageResponse();
                imageResponse.setId(file.getId());
                imageResponse.setName(file.getName());
                imageResponse.setGetWebContentLink(file.getWebContentLink());
                imageResponse.setWebViewLink(file.getWebViewLink());
                imageResponse.setGetImageMediaMetadata(file.getImageMediaMetadata());
                break;
            }
        }
        if (Objects.nonNull(imageResponse)) {
            return ResponseEntity.ok(imageResponse);
        } else {
            return ResponseEntity.status(404).body("Entity not found");
        }
    }

    @PostMapping(value = "/create/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createImageOnGoogleDrive(HttpServletResponse response, @RequestParam("multipartFile") MultipartFile multipartFile) throws IOException {
        Credential cred = flow.loadCredential(USER_IDENTIFIER_KEY);

        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, cred).setApplicationName(APPLICATION_NAME).build();
        File file = new File();
        file.setName(Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(0, multipartFile.getOriginalFilename().lastIndexOf(".")));
        java.io.File fileIO = new java.io.File(
                "C:\\Users\\dev\\Documents\\testgoogledrivespring\\src\\main\\resources\\static\\images\\"
                        + multipartFile.getOriginalFilename()
        );
        multipartFile.transferTo(fileIO);
        System.out.println(fileIO.getAbsolutePath());
        BufferedImage bImage = ImageIO.read(fileIO);
        ImageIO.write(bImage, "jpg", fileIO);
        FileContent content = new FileContent(multipartFile.getContentType(), fileIO);
        File uploadFile = drive.files().create(file, content).setFields("id, webContentLink, webViewLink, parents").execute();

        imageIdAndName.put(uploadFile.getId(), file.getName());
        System.out.println(imageIdAndName);
        Path imageServerPath = Paths.get(fileIO.getAbsolutePath());
        Files.delete(imageServerPath);

        String fileReference = String.format("{fileID: %s }", uploadFile.getId());
        response.getWriter().write(fileReference);
    }

}
