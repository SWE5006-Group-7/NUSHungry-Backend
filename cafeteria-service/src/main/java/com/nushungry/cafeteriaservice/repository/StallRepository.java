package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Stall;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long>, JpaSpecificationExecutor<Stall> {
    List<Stall> findByCafeteria_Id(Long cafeteriaId);

    // 修复懒加载问题: 使用 @EntityGraph 急切加载 Cafeteria 关联
    @EntityGraph(attributePaths = {"cafeteria"})
    @Query("SELECT s FROM Stall s")
    List<Stall> findAllWithCafeteria();

    // 修复单个 Stall 查询的懒加载问题
    @EntityGraph(attributePaths = {"cafeteria"})
    @Query("SELECT s FROM Stall s WHERE s.id = :id")
    Optional<Stall> findByIdWithCafeteria(Long id);

    // 简单更新方法也可用 save，但这里演示自定义更新语句
}


