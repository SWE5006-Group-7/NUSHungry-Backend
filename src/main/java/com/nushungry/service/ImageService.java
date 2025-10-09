package com.nushungry.service;

import com.nushungry.model.Cafeteria;
import com.nushungry.model.Image;
import com.nushungry.model.Stall;
import com.nushungry.repository.CafeteriaRepository;
import com.nushungry.repository.ImageRepository;
import com.nushungry.repository.StallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final CafeteriaRepository cafeteriaRepository;
    private final StallRepository stallRepository;

    // 图片上传目录，可以通过配置文件设置
    private static final String UPLOAD_DIR = "uploads/images/";

    public ImageService(ImageRepository imageRepository,
                       CafeteriaRepository cafeteriaRepository,
                       StallRepository stallRepository) {
        this.imageRepository = imageRepository;
        this.cafeteriaRepository = cafeteriaRepository;
        this.stallRepository = stallRepository;
    }

    @Transactional
    public Image uploadCafeteriaImage(Long cafeteriaId, MultipartFile file, String userId) throws IOException {
        Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                .orElseThrow(() -> new RuntimeException("Cafeteria not found"));

        String imageUrl = saveFile(file);

        Image image = new Image();
        image.setImageUrl(imageUrl);
        image.setUploadedBy(userId);
        image.setCafeteria(cafeteria);

        return imageRepository.save(image);
    }

    @Transactional
    public Image uploadStallImage(Long stallId, MultipartFile file, String userId) throws IOException {
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stall not found"));

        String imageUrl = saveFile(file);

        Image image = new Image();
        image.setImageUrl(imageUrl);
        image.setUploadedBy(userId);
        image.setStall(stall);

        return imageRepository.save(image);
    }

    public List<Image> getCafeteriaImages(Long cafeteriaId) {
        return imageRepository.findByCafeteriaId(cafeteriaId);
    }

    public List<Image> getStallImages(Long stallId) {
        return imageRepository.findByStallId(stallId);
    }

    private String saveFile(MultipartFile file) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;

        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // 返回访问路径
        return "/uploads/images/" + filename;
    }
}