package com.nushungry.service;

import com.nushungry.model.Cafeteria;
import com.nushungry.repository.CafeteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CafeteriaService {

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    public List<Cafeteria> findAll() {
        return cafeteriaRepository.findAll();
    }

    public Optional<Cafeteria> findById(Long id) {
        return cafeteriaRepository.findById(id);
    }

    public Cafeteria save(Cafeteria cafeteria) {
        if (cafeteria == null) {
            throw new IllegalArgumentException("Cafeteria must not be null");
        }
        return cafeteriaRepository.save(cafeteria);
    }

    public List<Cafeteria> findPopularCafeterias() {
        return cafeteriaRepository.findPopularCafeterias();
    }
}