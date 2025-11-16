package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Image;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.ImageRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 图片关联服务层
 * 负责管理图片与食堂、摊位的关联关系
 */
@Service
@Transactional
public class ImageService {

    private final ImageRepository imageRepository;
    private final CafeteriaRepository cafeteriaRepository;
    private final StallRepository stallRepository;

    public ImageService(ImageRepository imageRepository,
                       CafeteriaRepository cafeteriaRepository,
                       StallRepository stallRepository) {
        this.imageRepository = imageRepository;
        this.cafeteriaRepository = cafeteriaRepository;
        this.stallRepository = stallRepository;
    }

    /**
     * 关联图片到食堂
     */
    public Image addImageToCafeteria(Long cafeteriaId, String imageUrl, String thumbnailUrl,
                                   Image.ImageType type, String uploadedBy) {
        Optional<Cafeteria> cafeteriaOpt = cafeteriaRepository.findById(cafeteriaId);
        if (!cafeteriaOpt.isPresent()) {
            throw new IllegalArgumentException("食堂不存在: " + cafeteriaId);
        }

        Cafeteria cafeteria = cafeteriaOpt.get();
        Image image = new Image();
        image.setImageUrl(imageUrl);
        image.setThumbnailUrl(thumbnailUrl);
        image.setType(type);
        image.setUploadedBy(uploadedBy);
        image.setCafeteria(cafeteria);

        return imageRepository.save(image);
    }

    /**
     * 关联图片到摊位
     */
    public Image addImageToStall(Long stallId, String imageUrl, String thumbnailUrl,
                               Image.ImageType type, String uploadedBy) {
        Optional<Stall> stallOpt = stallRepository.findById(stallId);
        if (!stallOpt.isPresent()) {
            throw new IllegalArgumentException("摊位不存在: " + stallId);
        }

        Stall stall = stallOpt.get();
        Image image = new Image();
        image.setImageUrl(imageUrl);
        image.setThumbnailUrl(thumbnailUrl);
        image.setType(type);
        image.setUploadedBy(uploadedBy);
        image.setStall(stall);

        return imageRepository.save(image);
    }

    /**
     * 获取食堂的所有图片
     */
    @Transactional(readOnly = true)
    public List<Image> getCafeteriaImages(Long cafeteriaId) {
        // 验证食堂是否存在
        if (!cafeteriaRepository.existsById(cafeteriaId)) {
            throw new IllegalArgumentException("食堂不存在: " + cafeteriaId);
        }
        return imageRepository.findByCafeteria_Id(cafeteriaId);
    }

    /**
     * 获取摊位的所有图片
     */
    @Transactional(readOnly = true)
    public List<Image> getStallImages(Long stallId) {
        // 验证摊位是否存在
        if (!stallRepository.existsById(stallId)) {
            throw new IllegalArgumentException("摊位不存在: " + stallId);
        }
        return imageRepository.findByStall_Id(stallId);
    }

    /**
     * 获取食堂的指定类型图片
     */
    @Transactional(readOnly = true)
    public List<Image> getCafeteriaImagesByType(Long cafeteriaId, Image.ImageType type) {
        // 验证食堂是否存在
        if (!cafeteriaRepository.existsById(cafeteriaId)) {
            throw new IllegalArgumentException("食堂不存在: " + cafeteriaId);
        }
        return imageRepository.findByCafeteria_IdAndType(cafeteriaId, type);
    }

    /**
     * 获取摊位的指定类型图片
     */
    @Transactional(readOnly = true)
    public List<Image> getStallImagesByType(Long stallId, Image.ImageType type) {
        // 验证摊位是否存在
        if (!stallRepository.existsById(stallId)) {
            throw new IllegalArgumentException("摊位不存在: " + stallId);
        }
        return imageRepository.findByStall_IdAndType(stallId, type);
    }

    /**
     * 删除食堂图片关联（仅删除关联关系，不删除实际文件）
     */
    public void removeCafeteriaImage(Long cafeteriaId, Long imageId) {
        Optional<Image> imageOpt = imageRepository.findById(imageId);
        if (!imageOpt.isPresent()) {
            throw new IllegalArgumentException("图片不存在: " + imageId);
        }

        Image image = imageOpt.get();
        if (image.getCafeteria() == null || !image.getCafeteria().getId().equals(cafeteriaId)) {
            throw new IllegalArgumentException("图片不属于指定食堂");
        }

        // 解除关联关系
        image.setCafeteria(null);
        imageRepository.save(image);
    }

    /**
     * 删除摊位图片关联（仅删除关联关系，不删除实际文件）
     */
    public void removeStallImage(Long stallId, Long imageId) {
        Optional<Image> imageOpt = imageRepository.findById(imageId);
        if (!imageOpt.isPresent()) {
            throw new IllegalArgumentException("图片不存在: " + imageId);
        }

        Image image = imageOpt.get();
        if (image.getStall() == null || !image.getStall().getId().equals(stallId)) {
            throw new IllegalArgumentException("图片不属于指定摊位");
        }

        // 解除关联关系
        image.setStall(null);
        imageRepository.save(image);
    }

    /**
     * 彻底删除图片（删除数据库记录）
     */
    public void deleteImage(Long imageId) {
        if (!imageRepository.existsById(imageId)) {
            throw new IllegalArgumentException("图片不存在: " + imageId);
        }
        imageRepository.deleteById(imageId);
    }
}