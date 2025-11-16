package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CafeteriaRepositoryTest {

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @BeforeEach
    void setUp() {
        cafeteriaRepository.deleteAll();
    }

    @Test
    void save_and_find() {
        Cafeteria c = new Cafeteria();
        c.setName("Test Cafeteria");
        Cafeteria saved = cafeteriaRepository.save(c);
        assertThat(saved.getId()).isNotNull();
        var found = cafeteriaRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Cafeteria");
    }

    @Test
    void findAllSimple_ShouldReturnAllCafeterias() {
        // Given: 创建多个食堂
        Cafeteria c1 = new Cafeteria();
        c1.setName("Cafeteria 1");
        c1.setLocation("Location 1");

        Cafeteria c2 = new Cafeteria();
        c2.setName("Cafeteria 2");
        c2.setLocation("Location 2");

        Cafeteria c3 = new Cafeteria();
        c3.setName("Cafeteria 3");
        c3.setLocation("Location 3");

        cafeteriaRepository.save(c1);
        cafeteriaRepository.save(c2);
        cafeteriaRepository.save(c3);

        // When: 调用自定义查询
        List<Cafeteria> cafeterias = cafeteriaRepository.findAllSimple();

        // Then: 验证返回所有食堂
        assertThat(cafeterias).hasSize(3);
        assertThat(cafeterias)
                .extracting(Cafeteria::getName)
                .containsExactlyInAnyOrder("Cafeteria 1", "Cafeteria 2", "Cafeteria 3");
    }

    @Test
    void findAllSimple_WhenNoCafeterias_ShouldReturnEmptyList() {
        // When: 数据库为空
        List<Cafeteria> cafeterias = cafeteriaRepository.findAllSimple();

        // Then: 返回空列表
        assertThat(cafeterias).isEmpty();
    }

    @Test
    void findAllSimple_WhenSingleCafeteria_ShouldReturnOne() {
        // Given: 只有一个食堂
        Cafeteria c = new Cafeteria();
        c.setName("Single Cafeteria");
        cafeteriaRepository.save(c);

        // When: 调用自定义查询
        List<Cafeteria> cafeterias = cafeteriaRepository.findAllSimple();

        // Then: 返回单个食堂
        assertThat(cafeterias).hasSize(1);
        assertThat(cafeterias.get(0).getName()).isEqualTo("Single Cafeteria");
    }
}
