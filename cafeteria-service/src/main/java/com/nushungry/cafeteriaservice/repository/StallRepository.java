package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long> {
    List<Stall> findByCafeteria_Id(Long cafeteriaId);

    // 简单更新方法也可用 save，但这里演示自定义更新语句
}


