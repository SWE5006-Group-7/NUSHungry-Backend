package com.nushungry.specification;

import com.nushungry.dto.StallSearchRequest;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Stall动态查询Specification
 */
public class StallSpecification {

    /**
     * 根据搜索条件构建Specification
     */
    public static Specification<Stall> buildSpecification(StallSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 关键词搜索（摊位名称或菜系类型）
            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")), keyword);
                Predicate cuisinePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("cuisineType")), keyword);
                predicates.add(criteriaBuilder.or(namePredicate, cuisinePredicate));
            }

            // 2. 菜系类型筛选（多选）
            if (request.getCuisineTypes() != null && !request.getCuisineTypes().isEmpty()) {
                predicates.add(root.get("cuisineType").in(request.getCuisineTypes()));
            }

            // 3. 最低评分筛选
            if (request.getMinRating() != null && request.getMinRating() > 0) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("averageRating"), request.getMinRating()));
            }

            // 4. Halal筛选
            if (request.getHalalOnly() != null && request.getHalalOnly()) {
                predicates.add(criteriaBuilder.isNotNull(root.get("halalInfo")));
                predicates.add(criteriaBuilder.notEqual(root.get("halalInfo"), ""));
            }

            // 5. 食堂筛选
            if (request.getCafeteriaId() != null) {
                Join<Stall, Cafeteria> cafeteriaJoin = root.join("cafeteria", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(cafeteriaJoin.get("id"), request.getCafeteriaId()));
            }

            // 6. 距离筛选（如果提供了用户位置和最大距离）
            if (request.getUserLatitude() != null && request.getUserLongitude() != null
                && request.getMaxDistance() != null && request.getMaxDistance() > 0) {
                // 使用Haversine公式计算距离
                // 这里使用简化版本，实际应该使用数据库的地理空间函数
                Join<Stall, Cafeteria> cafeteriaJoin = root.join("cafeteria", JoinType.LEFT);

                // 注意：这是一个简化的距离过滤，实际生产环境应该使用PostGIS等空间数据库扩展
                // 这里暂时不实现精确的距离过滤，可以在Service层进行后处理
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 计算两点之间的距离（km）- Haversine公式
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（km）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
