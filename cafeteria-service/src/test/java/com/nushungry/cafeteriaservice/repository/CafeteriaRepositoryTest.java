package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CafeteriaRepositoryTest {

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

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
}
