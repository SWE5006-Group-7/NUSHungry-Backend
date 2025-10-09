package com.nushungry.repository;

import com.nushungry.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByCafeteriaId(Long cafeteriaId);
    List<Image> findByStallId(Long stallId);
    List<Image> findByUploadedBy(String uploadedBy);
}