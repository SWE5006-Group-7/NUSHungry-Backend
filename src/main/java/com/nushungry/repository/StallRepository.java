package com.nushungry.repository;

import com.nushungry.model.Stall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long> {

    List<Stall> findByCafeteriaId(Long cafeteriaId);
}