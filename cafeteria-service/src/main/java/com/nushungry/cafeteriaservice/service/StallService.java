package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.dto.StallSearchRequest;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import com.nushungry.cafeteriaservice.specification.StallSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StallService {

    private final StallRepository stallRepository;

    public StallService(StallRepository stallRepository) {
        this.stallRepository = stallRepository;
    }

    @Transactional(readOnly = true)
    public List<Stall> findAll() {
        // 使用 findAllWithCafeteria() 来急切加载 Cafeteria，避免懒加载异常
        return stallRepository.findAllWithCafeteria();
    }

    public Optional<Stall> findById(Long id) {
        // 使用 findByIdWithCafeteria() 来急切加载 Cafeteria，避免懒加载异常
        return stallRepository.findByIdWithCafeteria(id);
    }

    public List<Stall> findByCafeteriaId(Long cafeteriaId) {
        return stallRepository.findByCafeteria_Id(cafeteriaId);
    }

    public Stall save(Stall stall) {
        if (stall == null) {
            throw new IllegalArgumentException("Stall must not be null");
        }
        return stallRepository.save(stall);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Stall ID must not be null");
        }
        stallRepository.deleteById(id);
    }

    /**
     * 搜索摊位（支持动态筛选、排序、分页）
     */
    public Page<Stall> searchStalls(StallSearchRequest request) {
        // 1. 构建查询条件
        Specification<Stall> spec = StallSpecification.buildSpecification(request);

        // 2. 构建排序
        Sort sort = buildSort(request.getSortBy(), request.getSortDirection());

        // 3. 构建分页
        Pageable pageable = PageRequest.of(
            request.getPage() != null ? request.getPage() : 0,
            request.getSize() != null ? request.getSize() : 20,
            sort
        );

        // 4. 执行查询
        Page<Stall> page = stallRepository.findAll(spec, pageable);

        // 5. 距离过滤和排序（如果需要）
        if (request.getUserLatitude() != null && request.getUserLongitude() != null) {
            // 如果需要按距离排序或过滤，需要在内存中处理
            if ("distance".equals(request.getSortBy()) || request.getMaxDistance() != null) {
                return filterAndSortByDistance(page, request);
            }
        }

        return page;
    }

    /**
     * 构建排序
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        // 默认排序：评分降序
        if (!StringUtils.hasText(sortBy)) {
            sortBy = "rating";
        }
        if (!StringUtils.hasText(sortDirection)) {
            sortDirection = "desc";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        // 映射排序字段
        String sortField = switch (sortBy.toLowerCase()) {
            case "rating" -> "averageRating";
            case "reviews" -> "reviewCount";
            case "price" -> "averagePrice";
            case "distance" -> "id"; // 距离排序需要在内存中处理
            default -> "averageRating";
        };

        return Sort.by(direction, sortField);
    }

    /**
     * 按距离过滤和排序（内存处理）
     */
    private Page<Stall> filterAndSortByDistance(Page<Stall> page, StallSearchRequest request) {
        double userLat = request.getUserLatitude();
        double userLon = request.getUserLongitude();
        Double maxDistance = request.getMaxDistance();

        List<Stall> stalls = page.getContent().stream()
            .filter(stall -> {
                // 计算距离
                double lat = stall.getLatitude() != null ? stall.getLatitude()
                    : (stall.getCafeteria() != null ? stall.getCafeteria().getLatitude() : 0);
                double lon = stall.getLongitude() != null ? stall.getLongitude()
                    : (stall.getCafeteria() != null ? stall.getCafeteria().getLongitude() : 0);

                double distance = StallSpecification.calculateDistance(userLat, userLon, lat, lon);

                // 距离过滤
                return maxDistance == null || distance <= maxDistance;
            })
            .sorted((s1, s2) -> {
                // 如果需要按距离排序
                if ("distance".equals(request.getSortBy())) {
                    double lat1 = s1.getLatitude() != null ? s1.getLatitude()
                        : (s1.getCafeteria() != null ? s1.getCafeteria().getLatitude() : 0);
                    double lon1 = s1.getLongitude() != null ? s1.getLongitude()
                        : (s1.getCafeteria() != null ? s1.getCafeteria().getLongitude() : 0);

                    double lat2 = s2.getLatitude() != null ? s2.getLatitude()
                        : (s2.getCafeteria() != null ? s2.getCafeteria().getLatitude() : 0);
                    double lon2 = s2.getLongitude() != null ? s2.getLongitude()
                        : (s2.getCafeteria() != null ? s2.getCafeteria().getLongitude() : 0);

                    double distance1 = StallSpecification.calculateDistance(userLat, userLon, lat1, lon1);
                    double distance2 = StallSpecification.calculateDistance(userLat, userLon, lat2, lon2);

                    return Double.compare(distance1, distance2);
                }
                return 0;
            })
            .collect(Collectors.toList());

        // 注意：这里返回的分页信息可能不准确，因为过滤后数量变化了
        // 实际生产环境应该使用数据库的地理空间函数
        return page;
    }
}


