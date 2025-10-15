package com.nushungry.service;

import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.repository.CafeteriaRepository;
import com.nushungry.repository.StallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CafeteriaService {

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @Autowired
    private StallRepository stallRepository;

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

    public List<Stall> findStallsByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteriaId(cafeteriaId);
    }

    /**
     * 从 Cafeteria 下的所有 Stall 中聚合菜系标签
     */
    public Set<String> aggregateCuisineTags(Long cafeteriaId) {
        List<Stall> stalls = stallRepository.findByCafeteriaId(cafeteriaId);
        return stalls.stream()
                .map(Stall::getCuisineType)
                .filter(cuisineType -> cuisineType != null && !cuisineType.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 删除食堂(级联删除关联的摊位)
     */
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Cafeteria ID must not be null");
        }
        if (!cafeteriaRepository.existsById(id)) {
            throw new IllegalArgumentException("Cafeteria not found with id: " + id);
        }
        cafeteriaRepository.deleteById(id);
    }
}