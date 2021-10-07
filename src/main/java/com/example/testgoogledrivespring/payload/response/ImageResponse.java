package com.example.testgoogledrivespring.payload.response;

import com.google.api.services.drive.model.File;
import lombok.Data;

import java.util.List;

@Data
public class ImageResponse {

    private String id;

    private String name;

    private String getWebContentLink;

    private String webViewLink;

    private File.ImageMediaMetadata getImageMediaMetadata;

}
