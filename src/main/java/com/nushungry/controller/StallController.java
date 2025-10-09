package com.nushungry.controller;

import com.nushungry.model.Stall;
import com.nushungry.model.StallDetailDTO;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    @Autowired
    private StallService stallService;

    @Autowired
    private ImageService imageService;

    @GetMapping
    public List<Stall> getAllStalls() {
        return stallService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StallDetailDTO> getStallById(@PathVariable Long id) {
        return stallService.findById(id)
                .map(stall -> {
                    StallDetailDTO dto = new StallDetailDTO(
                        stall.getId(),
                        stall.getName(),
                        stall.getCuisineType(),
                        stall.getImageUrl(),
                        stall.getHalalInfo(),
                        stall.getContact()
                    );
                    // 只设置 cafeteria 的基本信息,避免 Hibernate 懒加载序列化问题
                    if (stall.getCafeteria() != null) {
                        dto.setCafeteriaId(stall.getCafeteria().getId());
                        dto.setCafeteriaName(stall.getCafeteria().getName());
                    }
                    dto.setReviews(stall.getReviews());
                    dto.setImages(imageService.getStallImages(id));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Stall createStall(@RequestBody Stall stall) {
        return stallService.save(stall);
    }

}