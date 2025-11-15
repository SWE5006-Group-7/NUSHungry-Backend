package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByCafeteria_Id(Long cafeteriaId);
    List<Image> findByStall_Id(Long stallId);
    List<Image> findByCafeteria_IdAndType(Long cafeteriaId, Image.ImageType type);
    List<Image> findByStall_IdAndType(Long stallId, Image.ImageType type);
    boolean existsByIdAndCafeteria_Id(Long imageId, Long cafeteriaId);
    boolean existsByIdAndStall_Id(Long imageId, Long stallId);
}


