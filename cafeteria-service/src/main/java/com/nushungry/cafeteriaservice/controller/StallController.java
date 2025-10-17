package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.service.StallService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    private final StallService stallService;

    public StallController(StallService stallService) {
        this.stallService = stallService;
    }

    @GetMapping
    public List<Stall> getAllStalls() {
        return stallService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stall> getStall(@PathVariable Long id) {
        return stallService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}


