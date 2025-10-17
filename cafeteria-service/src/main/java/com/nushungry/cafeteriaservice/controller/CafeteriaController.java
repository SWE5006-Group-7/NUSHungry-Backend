package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cafeterias")
public class CafeteriaController {

    private final CafeteriaService cafeteriaService;

    public CafeteriaController(CafeteriaService cafeteriaService) {
        this.cafeteriaService = cafeteriaService;
    }

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

    @GetMapping("/{id}/stalls")
    public List<Stall> getStallsByCafeteria(@PathVariable Long id) {
        return cafeteriaService.findStallsByCafeteriaId(id);
    }
}


