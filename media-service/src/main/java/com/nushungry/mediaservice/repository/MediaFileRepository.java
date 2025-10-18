package com.nushungry.mediaservice.repository;

import com.nushungry.mediaservice.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
}