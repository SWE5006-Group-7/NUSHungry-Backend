package com.nushungry.service;

import com.nushungry.config.FileStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;
    private final Path imageStorageLocation;
    private final Path thumbnailStorageLocation;

    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.imageStorageLocation = Paths.get(fileStorageProperties.getImageUploadPath())
                .toAbsolutePath().normalize();
        this.thumbnailStorageLocation = Paths.get(fileStorageProperties.getThumbnailUploadPath())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.imageStorageLocation);
            Files.createDirectories(this.thumbnailStorageLocation);
            log.info("文件存储目录创建成功: {}", this.imageStorageLocation);
            log.info("缩略图存储目录创建成功: {}", this.thumbnailStorageLocation);
        } catch (IOException ex) {
            log.error("无法创建文件存储目录", ex);
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    /**
     * 存储单个文件
     */
    public String storeFile(MultipartFile file) {
        return storeFile(file, false);
    }

    /**
     * 存储单个文件
     * @param file 上传的文件
     * @param organizeByDate 是否按日期组织文件
     * @return 文件的访问路径
     */
    public String storeFile(MultipartFile file, boolean organizeByDate) {
        // 验证文件
        validateFile(file);

        try {
            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);

            // 生成唯一文件名
            String filename = generateUniqueFilename(extension);

            // 确定存储路径
            Path targetLocation = imageStorageLocation;
            String urlPath = "/uploads/images/";

            if (organizeByDate) {
                // 按日期组织文件：uploads/images/2024/01/15/xxx.jpg
                String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                targetLocation = imageStorageLocation.resolve(datePath);
                Files.createDirectories(targetLocation);
                urlPath = "/uploads/images/" + datePath + "/";
            }

            // 保存文件
            Path destinationFile = targetLocation.resolve(filename);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件上传成功: {}", destinationFile);
            return urlPath + filename;

        } catch (IOException ex) {
            log.error("文件存储失败", ex);
            throw new RuntimeException("文件存储失败: " + file.getOriginalFilename(), ex);
        }
    }

    /**
     * 批量存储文件
     */
    public List<String> storeFiles(List<MultipartFile> files) {
        return storeFiles(files, false);
    }

    /**
     * 批量存储文件
     */
    public List<String> storeFiles(List<MultipartFile> files, boolean organizeByDate) {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String url = storeFile(file, organizeByDate);
                uploadedUrls.add(url);
            } catch (Exception e) {
                log.error("批量上传中文件上传失败: {}", file.getOriginalFilename(), e);
                // 继续处理其他文件
            }
        }

        return uploadedUrls;
    }

    /**
     * 删除文件
     * @param fileUrl 文件的URL路径（如 /uploads/images/xxx.jpg）
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename == null) {
                log.warn("无效的文件URL: {}", fileUrl);
                return false;
            }

            // 构建文件路径
            Path filePath = imageStorageLocation.resolve(filename).normalize();

            // 安全检查：确保文件在允许的目录内
            if (!filePath.startsWith(imageStorageLocation)) {
                log.warn("试图删除不安全的文件路径: {}", filePath);
                return false;
            }

            // 删除文件
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("文件删除成功: {}", filePath);

                // 尝试删除对应的缩略图
                deleteThumbnail(filename);
            } else {
                log.warn("文件不存在: {}", filePath);
            }

            return deleted;

        } catch (IOException ex) {
            log.error("文件删除失败: {}", fileUrl, ex);
            return false;
        }
    }

    /**
     * 删除缩略图
     */
    private void deleteThumbnail(String filename) {
        try {
            Path thumbnailPath = thumbnailStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(thumbnailPath);
            log.info("缩略图删除成功: {}", thumbnailPath);
        } catch (IOException ex) {
            log.warn("缩略图删除失败: {}", filename, ex);
        }
    }

    /**
     * 批量删除文件
     */
    public List<String> deleteFiles(List<String> fileUrls) {
        List<String> deletedFiles = new ArrayList<>();

        for (String fileUrl : fileUrls) {
            if (deleteFile(fileUrl)) {
                deletedFiles.add(fileUrl);
            }
        }

        return deletedFiles;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new IllegalArgumentException(
                String.format("文件大小超过限制: %d MB",
                    fileStorageProperties.getMaxFileSize() / (1024 * 1024))
            );
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new IllegalArgumentException(
                "不支持的文件类型: " + contentType +
                "，允许的类型: " + Arrays.toString(fileStorageProperties.getAllowedImageTypes())
            );
        }

        // 验证文件名
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new IllegalArgumentException("无效的文件名: " + filename);
        }
    }

    /**
     * 检查是否为允许的图片类型
     */
    private boolean isAllowedImageType(String contentType) {
        return Arrays.asList(fileStorageProperties.getAllowedImageTypes()).contains(contentType);
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() +
               (extension != null && !extension.isEmpty() ? "." + extension : "");
    }

    /**
     * 从URL中提取文件名
     * 例如: /uploads/images/2024/01/15/xxx.jpg -> 2024/01/15/xxx.jpg
     */
    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }

        // 移除URL前缀 /uploads/images/
        String prefix = "/uploads/images/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }

        return null;
    }

    /**
     * 获取图片存储位置
     */
    public Path getImageStorageLocation() {
        return imageStorageLocation;
    }

    /**
     * 获取缩略图存储位置
     */
    public Path getThumbnailStorageLocation() {
        return thumbnailStorageLocation;
    }
}
