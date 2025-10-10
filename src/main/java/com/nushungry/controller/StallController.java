package com.nushungry.controller;

import com.nushungry.model.Stall;
import com.nushungry.model.StallDetailDTO;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    @Autowired
    private StallService stallService;

    @Autowired
    private ImageService imageService;

    @GetMapping
    public List<Map<String, Object>> getAllStalls() {
        List<Stall> stalls = stallService.findAll();
        // 手动构建包含cafeteria信息的响应，解决@JsonBackReference序列化问题
        return stalls.stream().map(stall -> {
            Map<String, Object> stallData = new HashMap<>();
            stallData.put("id", stall.getId());
            stallData.put("name", stall.getName());
            stallData.put("cuisineType", stall.getCuisineType());
            stallData.put("cuisine", stall.getCuisineType()); // 前端使用cuisine字段
            stallData.put("imageUrl", stall.getImageUrl());
            stallData.put("halalInfo", stall.getHalalInfo());
            stallData.put("halal", stall.getHalalInfo() != null && !stall.getHalalInfo().isEmpty());
            stallData.put("contact", stall.getContact());
            stallData.put("averageRating", stall.getAverageRating());
            stallData.put("reviewCount", stall.getReviewCount());

            // 添加cafeteria信息
            if (stall.getCafeteria() != null) {
                Map<String, Object> cafeteriaData = new HashMap<>();
                cafeteriaData.put("id", stall.getCafeteria().getId());
                cafeteriaData.put("name", stall.getCafeteria().getName());
                cafeteriaData.put("location", stall.getCafeteria().getLocation());
                stallData.put("cafeteria", cafeteriaData);
                stallData.put("cafeteriaName", stall.getCafeteria().getName()); // 前端使用cafeteriaName字段
            } else {
                stallData.put("cafeteria", null);
                stallData.put("cafeteriaName", null);
            }

            return stallData;
        }).collect(Collectors.toList());
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
