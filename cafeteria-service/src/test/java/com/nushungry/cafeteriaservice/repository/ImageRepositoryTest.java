package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Image;
import com.nushungry.cafeteriaservice.model.Stall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ImageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @Autowired
    private StallRepository stallRepository;

    private Cafeteria cafeteria;
    private Stall stall;

    @BeforeEach
    void setUp() {
        cafeteria = new Cafeteria();
        cafeteria.setName("Test Cafeteria");
        cafeteria.setLocation("Test Location");
        cafeteria = cafeteriaRepository.save(cafeteria);

        stall = new Stall();
        stall.setName("Test Stall");
        stall.setCafeteria(cafeteria);
        stall = stallRepository.save(stall);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByCafeteria_Id_ShouldReturnImagesForCafeteria() {
        Image image1 = new Image();
        image1.setImageUrl("/images/cafe1.jpg");
        image1.setCafeteria(cafeteria);
        imageRepository.save(image1);

        Image image2 = new Image();
        image2.setImageUrl("/images/cafe2.jpg");
        image2.setCafeteria(cafeteria);
        imageRepository.save(image2);

        entityManager.flush();
        entityManager.clear();

        List<Image> images = imageRepository.findByCafeteria_Id(cafeteria.getId());

        assertThat(images).hasSize(2);
        assertThat(images).extracting(Image::getImageUrl)
                .containsExactlyInAnyOrder("/images/cafe1.jpg", "/images/cafe2.jpg");
    }

    @Test
    void findByStall_Id_ShouldReturnImagesForStall() {
        Image image1 = new Image();
        image1.setImageUrl("/images/stall1.jpg");
        image1.setStall(stall);
        imageRepository.save(image1);

        Image image2 = new Image();
        image2.setImageUrl("/images/stall2.jpg");
        image2.setStall(stall);
        imageRepository.save(image2);

        entityManager.flush();
        entityManager.clear();

        List<Image> images = imageRepository.findByStall_Id(stall.getId());

        assertThat(images).hasSize(2);
        assertThat(images).extracting(Image::getImageUrl)
                .containsExactlyInAnyOrder("/images/stall1.jpg", "/images/stall2.jpg");
    }

    @Test
    void findByCafeteria_Id_WhenNoImages_ShouldReturnEmptyList() {
        List<Image> images = imageRepository.findByCafeteria_Id(cafeteria.getId());

        assertThat(images).isEmpty();
    }

    @Test
    void findByStall_Id_WhenNoImages_ShouldReturnEmptyList() {
        List<Image> images = imageRepository.findByStall_Id(stall.getId());

        assertThat(images).isEmpty();
    }

    @Test
    void save_ShouldPersistImageWithCafeteria() {
        Image image = new Image();
        image.setImageUrl("/images/test.jpg");
        image.setThumbnailUrl("/images/thumb.jpg");
        image.setType(Image.ImageType.PHOTO);
        image.setCafeteria(cafeteria);

        Image saved = imageRepository.save(image);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getImageUrl()).isEqualTo("/images/test.jpg");
        assertThat(saved.getUploadedAt()).isNotNull();
    }

    @Test
    void save_ShouldPersistImageWithStall() {
        Image image = new Image();
        image.setImageUrl("/images/menu.jpg");
        image.setType(Image.ImageType.MENU);
        image.setStall(stall);

        Image saved = imageRepository.save(image);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo(Image.ImageType.MENU);
    }
}
