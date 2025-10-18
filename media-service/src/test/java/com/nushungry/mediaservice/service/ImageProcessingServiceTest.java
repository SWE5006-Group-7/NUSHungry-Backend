package com.nushungry.mediaservice.service;

import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.repository.MediaFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ImageProcessingServiceTest {

    @Autowired
    private ImageProcessingService service;

    @MockBean
    private MediaFileRepository repository;

    @Test
    void testStoreFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", "test content".getBytes()
        );

        MediaFile mockMediaFile = new MediaFile();
        mockMediaFile.setFileName("test.jpg");
        when(repository.save(any(MediaFile.class))).thenReturn(mockMediaFile);

        MediaFile savedFile = service.storeFile(file);

        assertNotNull(savedFile);
        assertEquals("test.jpg", savedFile.getFileName());
    }
}