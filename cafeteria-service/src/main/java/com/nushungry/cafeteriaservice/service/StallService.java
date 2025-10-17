package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StallService {

    private final StallRepository stallRepository;

    public StallService(StallRepository stallRepository) {
        this.stallRepository = stallRepository;
    }

    public List<Stall> findAll() {
        return stallRepository.findAll();
    }

    public Optional<Stall> findById(Long id) {
        return stallRepository.findById(id);
    }

    public List<Stall> findByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteria_Id(cafeteriaId);
    }

    public Stall save(Stall stall) {
        if (stall == null) {
            throw new IllegalArgumentException("Stall must not be null");
        }
        return stallRepository.save(stall);
    }
}


