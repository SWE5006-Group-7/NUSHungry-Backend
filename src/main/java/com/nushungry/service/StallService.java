package com.nushungry.service;

import com.nushungry.model.Stall;
import com.nushungry.repository.StallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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


}