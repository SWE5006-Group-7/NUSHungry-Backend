package com.nushungry.mediaservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fileName;
    private String url;
    private String contentType;
    private Long size;
}