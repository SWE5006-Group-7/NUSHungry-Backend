package com.nushungry.controller;

import com.nushungry.model.Stall;
import com.nushungry.service.StallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    @Autowired
    private StallService stallService;

    @GetMapping
    public List<Stall> getAllStalls() {
        return stallService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stall> getStallById(@PathVariable Long id) {
        return stallService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Stall createStall(@RequestBody Stall stall) {
        return stallService.save(stall);
    }

}