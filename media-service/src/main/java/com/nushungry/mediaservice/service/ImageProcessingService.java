package com.nushungry.mediaservice.service;

import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class ImageProcessingService {
    @Value("${media.storage.path}")
    private String storagePath;

    private final MediaFileRepository repository;

    public ImageProcessingService(MediaFileRepository repository) {
        this.repository = repository;
    }

    public MediaFile storeFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 判断 storagePath 是绝对路径还是相对路径
        File storageDir = new File(storagePath);
        if (!storageDir.isAbsolute()) {
            // 相对路径：拼接到工作目录
            String basePath = System.getProperty("user.dir");
            storageDir = new File(basePath, storagePath);
        }

        File dest = new File(storageDir, fileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(fileName);
        mediaFile.setUrl("/media/" + fileName);
        mediaFile.setContentType(file.getContentType());
        mediaFile.setSize(file.getSize());
        return repository.save(mediaFile);
    }
}