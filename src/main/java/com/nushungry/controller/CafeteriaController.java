package com.nushungry.controller;

import com.nushungry.model.Cafeteria;
import com.nushungry.service.CafeteriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaController {

    @Autowired
    private CafeteriaService cafeteriaService;

    @GetMapping
    public List<Cafeteria> getAllCafeterias() {
        return cafeteriaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cafeteria> getCafeteriaById(@PathVariable Long id) {
        return cafeteriaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Cafeteria createCafeteria(@RequestBody Cafeteria cafeteria) {
        return cafeteriaService.save(cafeteria);
    }

    @GetMapping("/popular")
    public List<Cafeteria> getPopularCafeterias() {
        return cafeteriaService.findPopularCafeterias();
    }
}