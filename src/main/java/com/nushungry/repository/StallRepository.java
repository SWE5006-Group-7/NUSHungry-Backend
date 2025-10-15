package com.nushungry.repository;

import com.nushungry.model.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long>, JpaSpecificationExecutor<Stall> {

    List<Stall> findByCafeteriaId(Long cafeteriaId);

    /**
     * 统计指定时间之前创建的摊位数量
     */
    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计指定时间范围内创建的摊位数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}