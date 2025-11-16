package com.nushungry.mediaservice.repository;

import com.nushungry.mediaservice.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long>, JpaSpecificationExecutor<MediaFile> {

    long countByType(String type);

    long countByUploadedBy(String uploadedBy);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(m.size) FROM MediaFile m")
    Long getTotalSize();

    @Query("SELECT m.type, COUNT(m) FROM MediaFile m GROUP BY m.type")
    List<Object[]> countByTypeGroupBy();

    @Query("SELECT m.uploadedBy, COUNT(m) FROM MediaFile m GROUP BY m.uploadedBy")
    List<Object[]> countByUploadedByGroupBy();

    /**
     * 根据URL查找媒体文件
     */
    Optional<MediaFile> findByUrl(String url);
}