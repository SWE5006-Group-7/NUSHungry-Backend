package com.nushungry.controller;

import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.model.CafeteriaDetailDTO;
import com.nushungry.service.CafeteriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<CafeteriaDetailDTO> getCafeteriaById(@PathVariable Long id) {
        return cafeteriaService.findById(id)
                .map(cafeteria -> {
                    List<Stall> stalls = cafeteriaService.findStallsByCafeteriaId(id);
                    CafeteriaDetailDTO dto = new CafeteriaDetailDTO(
                        cafeteria.getId(),
                        cafeteria.getName(),
                        cafeteria.getDescription(),
                        cafeteria.getLocation(),
                        cafeteria.getLatitude(),
                        cafeteria.getLongitude(),
                        cafeteria.getImageUrl(),
                        cafeteria.getTermTimeOpeningHours(),
                        cafeteria.getVacationOpeningHours(),
                        cafeteria.getNearestBusStop(),
                        cafeteria.getNearestCarpark(),
                        cafeteria.getHalalInfo(),
                        cafeteria.getSeatingCapacity()
                    );
                    dto.setStalls(stalls);
                    return ResponseEntity.ok(dto);
                })
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

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return error;
    }
}