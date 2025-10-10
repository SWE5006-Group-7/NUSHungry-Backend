package com.nushungry.service;

import com.nushungry.model.Stall;
import com.nushungry.repository.StallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class StallService {

    @Autowired
    private StallRepository stallRepository;

    public List<Stall> findAll() {
        return stallRepository.findAll();
    }

    public Optional<Stall> findById(Long id) {
        return stallRepository.findById(id);
    }

    public Stall save(Stall stall) {
        if (stall == null) {
            throw new IllegalArgumentException("Stall must not be null");
        }
        return stallRepository.save(stall);
    }

    public void deleteById(Long id) {
        if (!stallRepository.existsById(id)) {
            throw new IllegalArgumentException("Stall not found with id: " + id);
        }
        stallRepository.deleteById(id);
    }

    public List<Stall> findByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteriaId(cafeteriaId);
    }

}