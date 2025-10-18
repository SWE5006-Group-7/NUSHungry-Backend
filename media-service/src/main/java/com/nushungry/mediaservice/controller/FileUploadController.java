package com.nushungry.mediaservice.controller;

import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.service.ImageProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
public class FileUploadController {
    private final ImageProcessingService service;

    public FileUploadController(ImageProcessingService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaFile> upload(@RequestParam("file") MultipartFile file) throws Exception {
        MediaFile saved = service.storeFile(file);
        return ResponseEntity.ok(saved);
    }
}