package com.example.testgoogledrivespring.controller;

import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@RestController
public class VideoController {

    @PostMapping(value = "/save/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String saveVideoInLocal(@RequestParam("videoFile") MultipartFile videoFile) {
        String dirName = "C:\\Users\\dev\\Documents\\testgoogledrivespring\\src\\main\\resources\\static\\images";
        try {
            System.out.println(videoFile.getOriginalFilename());
            saveFileFromUrlWithJavaIO(
                    dirName + "\\" + videoFile.getOriginalFilename(),
                    videoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dirName;
    }

    public static void saveFileFromUrlWithJavaIO(String fileName, MultipartFile videoFile)
            throws IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(videoFile.getInputStream());
            fout = new FileOutputStream(fileName);
            byte[] data = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null)
                in.close();
            if (fout != null)
                fout.close();
        }
    }

    public static void saveFileFromUrlWithCommonsIO(String fileName,
                                                    String fileUrl) throws MalformedURLException, IOException {
        FileUtils.copyURLToFile(new URL(fileUrl), new File(fileName));
    }

}
