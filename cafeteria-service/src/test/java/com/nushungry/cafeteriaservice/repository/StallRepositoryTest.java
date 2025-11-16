package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StallRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    private Cafeteria cafeteria1;
    private Cafeteria cafeteria2;

    @BeforeEach
    void setUp() {
        // 创建测试食堂
        cafeteria1 = new Cafeteria();
        cafeteria1.setName("Cafeteria 1");
        cafeteria1.setLocation("Location 1");
        cafeteria1 = cafeteriaRepository.save(cafeteria1);

        cafeteria2 = new Cafeteria();
        cafeteria2.setName("Cafeteria 2");
        cafeteria2.setLocation("Location 2");
        cafeteria2 = cafeteriaRepository.save(cafeteria2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByCafeteria_Id_ShouldReturnStallsForCafeteria() {
        // 创建档口
        Stall stall1 = new Stall();
        stall1.setName("Stall 1");
        stall1.setCafeteria(cafeteria1);
        stallRepository.save(stall1);

        Stall stall2 = new Stall();
        stall2.setName("Stall 2");
        stall2.setCafeteria(cafeteria1);
        stallRepository.save(stall2);

        Stall stall3 = new Stall();
        stall3.setName("Stall 3");
        stall3.setCafeteria(cafeteria2);
        stallRepository.save(stall3);

        entityManager.flush();
        entityManager.clear();

        // 查询 cafeteria1 的档口
        List<Stall> stalls = stallRepository.findByCafeteria_Id(cafeteria1.getId());

        assertThat(stalls).hasSize(2);
        assertThat(stalls).extracting(Stall::getName).containsExactlyInAnyOrder("Stall 1", "Stall 2");
    }

    @Test
    void findByCafeteria_Id_WhenNoStalls_ShouldReturnEmptyList() {
        List<Stall> stalls = stallRepository.findByCafeteria_Id(cafeteria1.getId());

        assertThat(stalls).isEmpty();
    }

    @Test
    void findByCafeteria_Id_WhenInvalidId_ShouldReturnEmptyList() {
        List<Stall> stalls = stallRepository.findByCafeteria_Id(999L);

        assertThat(stalls).isEmpty();
    }

    @Test
    void save_ShouldPersistStall() {
        Stall stall = new Stall();
        stall.setName("Test Stall");
        stall.setCuisineType("Chinese");
        stall.setHalalInfo("Halal");
        stall.setContact("12345678");
        stall.setCafeteria(cafeteria1);

        Stall saved = stallRepository.save(stall);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Stall");
        assertThat(saved.getCuisineType()).isEqualTo("Chinese");
        assertThat(saved.getHalalInfo()).isEqualTo("Halal");
    }

    @Test
    void findById_ShouldLoadCafeteriaAssociation() {
        Stall stall = new Stall();
        stall.setName("Test Stall");
        stall.setCafeteria(cafeteria1);
        Stall saved = stallRepository.save(stall);

        entityManager.flush();
        entityManager.clear();

        Stall found = stallRepository.findById(saved.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getCafeteria()).isNotNull();
        assertThat(found.getCafeteria().getName()).isEqualTo("Cafeteria 1");
    }

    @Test
    void delete_ShouldRemoveStall() {
        Stall stall = new Stall();
        stall.setName("Test Stall");
        stall.setCafeteria(cafeteria1);
        Stall saved = stallRepository.save(stall);

        entityManager.flush();
        entityManager.clear();

        stallRepository.deleteById(saved.getId());

        assertThat(stallRepository.findById(saved.getId())).isEmpty();
    }
}
