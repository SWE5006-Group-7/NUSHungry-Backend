package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cafeterias")
public class AdminCafeteriaController {

    private final CafeteriaService cafeteriaService;

    public AdminCafeteriaController(CafeteriaService cafeteriaService) {
        this.cafeteriaService = cafeteriaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Cafeteria cafeteria) {
        Cafeteria saved = cafeteriaService.save(cafeteria);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "cafeteria", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Cafeteria cafeteria) {
        if (cafeteriaService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Not found"));
        }
        cafeteria.setId(id);
        Cafeteria saved = cafeteriaService.save(cafeteria);
        return ResponseEntity.ok(Map.of("success", true, "cafeteria", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        cafeteriaService.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}


