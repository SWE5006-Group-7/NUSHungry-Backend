package com.nushungry.service;

import com.nushungry.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 图片处理服务
 * 提供图片压缩、缩略图生成、格式转换等功能
 */
@Slf4j
@Service
public class ImageProcessingService {

    private final FileStorageProperties fileStorageProperties;
    private final FileStorageService fileStorageService;

    public ImageProcessingService(FileStorageProperties fileStorageProperties,
                                 FileStorageService fileStorageService) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 处理上传的图片（压缩并生成缩略图）
     * @param file 上传的图片文件
     * @param generateThumbnail 是否生成缩略图
     * @return 包含原图URL和缩略图URL的结果
     */
    public ImageProcessingResult processImage(MultipartFile file, boolean generateThumbnail) throws IOException {
        // 1. 压缩并保存原图
        String originalUrl = compressAndSave(file);

        // 2. 生成缩略图（如果需要）
        String thumbnailUrl = null;
        if (generateThumbnail && fileStorageProperties.isGenerateThumbnail()) {
            thumbnailUrl = generateThumbnail(file);
        }

        return new ImageProcessingResult(originalUrl, thumbnailUrl);
    }

    /**
     * 压缩并保存图片
     */
    private String compressAndSave(MultipartFile file) throws IOException {
        try {
            // 读取图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new IOException("无法读取图片文件");
            }

            // 获取原始尺寸
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 如果图片太大，进行等比例缩放（最大宽度2048px）
            int maxWidth = 2048;
            int maxHeight = 2048;

            int targetWidth = originalWidth;
            int targetHeight = originalHeight;

            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
                targetWidth = (int) (originalWidth * scale);
                targetHeight = (int) (originalHeight * scale);
            }

            // 创建临时文件用于存储压缩后的图片
            Path tempFile = Files.createTempFile("compressed_", ".jpg");

            // 使用 Thumbnailator 进行压缩
            Thumbnails.of(originalImage)
                    .size(targetWidth, targetHeight)
                    .outputQuality(fileStorageProperties.getCompressionQuality())
                    .outputFormat("jpg")
                    .toFile(tempFile.toFile());

            // 将压缩后的文件转换为 MultipartFile 并保存
            byte[] compressedBytes = Files.readAllBytes(tempFile);

            // 使用 FileStorageService 保存文件
            String url = fileStorageService.storeFile(
                new ByteArrayMultipartFile(compressedBytes, file.getOriginalFilename()),
                true // 按日期组织
            );

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            log.info("图片压缩成功: 原始尺寸 {}x{}, 压缩后尺寸 {}x{}, URL: {}",
                    originalWidth, originalHeight, targetWidth, targetHeight, url);

            return url;

        } catch (IOException ex) {
            log.error("图片压缩失败", ex);
            // 如果压缩失败，直接保存原图
            return fileStorageService.storeFile(file, true);
        }
    }

    /**
     * 生成缩略图
     */
    private String generateThumbnail(MultipartFile file) throws IOException {
        try {
            // 读取图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                throw new IOException("无法读取图片文件");
            }

            int thumbnailWidth = fileStorageProperties.getThumbnailWidth();
            int thumbnailHeight = fileStorageProperties.getThumbnailHeight();

            // 创建临时文件用于存储缩略图
            Path tempFile = Files.createTempFile("thumbnail_", ".jpg");

            // 使用 Thumbnailator 生成缩略图（保持宽高比，裁剪中心部分）
            Thumbnails.of(originalImage)
                    .size(thumbnailWidth, thumbnailHeight)
                    .crop(net.coobird.thumbnailator.geometry.Positions.CENTER)
                    .outputQuality(0.8f)
                    .outputFormat("jpg")
                    .toFile(tempFile.toFile());

            // 将缩略图文件转换为 MultipartFile
            byte[] thumbnailBytes = Files.readAllBytes(tempFile);

            // 保存到缩略图目录
            Path thumbnailStorageLocation = fileStorageService.getThumbnailStorageLocation();
            String filename = java.util.UUID.randomUUID().toString() + ".jpg";
            Path targetLocation = thumbnailStorageLocation.resolve(filename);

            Files.write(targetLocation, thumbnailBytes);

            // 删除临时文件
            Files.deleteIfExists(tempFile);

            String thumbnailUrl = "/uploads/thumbnails/" + filename;
            log.info("缩略图生成成功: {}x{}, URL: {}", thumbnailWidth, thumbnailHeight, thumbnailUrl);

            return thumbnailUrl;

        } catch (IOException ex) {
            log.error("缩略图生成失败", ex);
            return null;
        }
    }

    /**
     * 验证是否为有效的图片文件
     */
    public boolean isValidImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            return image != null;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 获取图片信息（尺寸、格式等）
     */
    public ImageInfo getImageInfo(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (image == null) {
            throw new IOException("无法读取图片文件");
        }

        return new ImageInfo(
            image.getWidth(),
            image.getHeight(),
            file.getContentType(),
            file.getSize()
        );
    }

    /**
     * 图片处理结果
     */
    public static class ImageProcessingResult {
        private final String originalUrl;
        private final String thumbnailUrl;

        public ImageProcessingResult(String originalUrl, String thumbnailUrl) {
            this.originalUrl = originalUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getOriginalUrl() {
            return originalUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
    }

    /**
     * 图片信息
     */
    public static class ImageInfo {
        private final int width;
        private final int height;
        private final String contentType;
        private final long size;

        public ImageInfo(int width, int height, String contentType, long size) {
            this.width = width;
            this.height = height;
            this.contentType = contentType;
            this.size = size;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * 字节数组转 MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;

        public ByteArrayMultipartFile(byte[] content, String name) {
            this.content = content;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return "image/jpeg";
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }
}
