package com.nushungry.cafeteriaservice.repository;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeteriaRepository extends JpaRepository<Cafeteria, Long> {

    @Query("SELECT c FROM Cafeteria c")
    List<Cafeteria> findAllSimple();
}


