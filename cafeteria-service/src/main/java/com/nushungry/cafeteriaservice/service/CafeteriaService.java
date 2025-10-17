package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CafeteriaService {

    private final CafeteriaRepository cafeteriaRepository;
    private final StallRepository stallRepository;

    public CafeteriaService(CafeteriaRepository cafeteriaRepository, StallRepository stallRepository) {
        this.cafeteriaRepository = cafeteriaRepository;
        this.stallRepository = stallRepository;
    }

    public List<Cafeteria> findAll() {
        return cafeteriaRepository.findAll();
    }

    public Optional<Cafeteria> findById(Long id) {
        return cafeteriaRepository.findById(id);
    }

    public List<Stall> findStallsByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteria_Id(cafeteriaId);
    }

    public Cafeteria save(Cafeteria cafeteria) {
        if (cafeteria == null) {
            throw new IllegalArgumentException("Cafeteria must not be null");
        }
        return cafeteriaRepository.save(cafeteria);
    }

    public void deleteById(Long id) {
        cafeteriaRepository.deleteById(id);
    }
}


