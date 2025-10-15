package com.nushungry.service;

import com.nushungry.dto.StallSearchRequest;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.repository.StallRepository;
import com.nushungry.specification.StallSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
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

        // 自动填充坐标：如果stall有关联的cafeteria且自身没有设置坐标，则从cafeteria复制坐标
        autoFillCoordinates(stall);

        return stallRepository.save(stall);
    }

    /**
     * 自动填充摊位坐标
     * 如果摊位关联了cafeteria且自身没有设置坐标，则从cafeteria复制坐标
     */
    private void autoFillCoordinates(Stall stall) {
        // 如果stall已经有坐标，不需要自动填充
        if (stall.getLatitude() != null && stall.getLongitude() != null) {
            return;
        }

        // 如果stall有关联的cafeteria，从cafeteria复制坐标
        Cafeteria cafeteria = stall.getCafeteria();
        if (cafeteria != null && cafeteria.getLatitude() != 0.0 && cafeteria.getLongitude() != 0.0) {
            stall.setLatitude(cafeteria.getLatitude());
            stall.setLongitude(cafeteria.getLongitude());
        }
    }

    public void deleteById(Long id) {
        if (!stallRepository.existsById(id)) {
            throw new IllegalArgumentException("Stall not found with id: " + id);
        }
        stallRepository.deleteById(id);
    }

    public List<Stall> findByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteriaId(cafeteriaId);
    }

    /**
     * 搜索和筛选摊位
     */
    public Page<Stall> searchStalls(StallSearchRequest request) {
        // 构建Specification
        Specification<Stall> spec = StallSpecification.buildSpecification(request);

        // 构建排序
        Sort sort = buildSort(request);

        // 构建分页
        Pageable pageable = PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 20,
            sort
        );

        // 执行查询
        Page<Stall> page = stallRepository.findAll(spec, pageable);

        // 如果需要按距离排序，需要在查询后处理
        if ("distance".equals(request.getSortBy()) &&
            request.getUserLatitude() != null &&
            request.getUserLongitude() != null) {
            List<Stall> sortedStalls = page.getContent().stream()
                .filter(stall -> stall.getCafeteria() != null)
                .sorted(Comparator.comparingDouble(stall -> {
                    double distance = StallSpecification.calculateDistance(
                        request.getUserLatitude(),
                        request.getUserLongitude(),
                        stall.getCafeteria().getLatitude(),
                        stall.getCafeteria().getLongitude()
                    );
                    return distance;
                }))
                .collect(Collectors.toList());

            // 根据最大距离筛选
            if (request.getMaxDistance() != null && request.getMaxDistance() > 0) {
                sortedStalls = sortedStalls.stream()
                    .filter(stall -> {
                        double distance = StallSpecification.calculateDistance(
                            request.getUserLatitude(),
                            request.getUserLongitude(),
                            stall.getCafeteria().getLatitude(),
                            stall.getCafeteria().getLongitude()
                        );
                        return distance <= request.getMaxDistance();
                    })
                    .collect(Collectors.toList());
            }

            // 注意：这里返回的分页信息可能不准确，因为距离筛选是在查询后进行的
            // 生产环境应该使用数据库的地理空间函数
        }

        return page;
    }

    /**
     * 构建排序条件
     */
    private Sort buildSort(StallSearchRequest request) {
        if (!StringUtils.hasText(request.getSortBy())) {
            return Sort.by(Sort.Direction.DESC, "averageRating");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        switch (request.getSortBy().toLowerCase()) {
            case "rating":
                return Sort.by(direction, "averageRating");
            case "reviews":
                return Sort.by(direction, "reviewCount");
            case "name":
                return Sort.by(direction, "name");
            case "distance":
                // 距离排序需要在查询后处理
                return Sort.unsorted();
            default:
                return Sort.by(Sort.Direction.DESC, "averageRating");
        }
    }
}