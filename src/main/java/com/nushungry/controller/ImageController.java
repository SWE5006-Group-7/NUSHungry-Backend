package com.nushungry.controller;

import com.nushungry.model.Image;
import com.nushungry.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/cafeteria/{cafeteriaId}")
    public ResponseEntity<Image> uploadCafeteriaImage(
            @PathVariable Long cafeteriaId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        try {
            Image image = imageService.uploadCafeteriaImage(cafeteriaId, file, userId);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/stall/{stallId}")
    public ResponseEntity<Image> uploadStallImage(
            @PathVariable Long stallId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {
        try {
            Image image = imageService.uploadStallImage(stallId, file, userId);
            return ResponseEntity.ok(image);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cafeteria/{cafeteriaId}")
    public ResponseEntity<List<Image>> getCafeteriaImages(@PathVariable Long cafeteriaId) {
        List<Image> images = imageService.getCafeteriaImages(cafeteriaId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/stall/{stallId}")
    public ResponseEntity<List<Image>> getStallImages(@PathVariable Long stallId) {
        List<Image> images = imageService.getStallImages(stallId);
        return ResponseEntity.ok(images);
    }
}