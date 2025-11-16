package com.nushungry.repository;

import java.util.Optional;
import org.springframework.data.repository.query.Param;
import com.nushungry.model.Cafeteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeteriaRepository extends JpaRepository<Cafeteria, Long> {

    @Query("SELECT c FROM Cafeteria c JOIN c.stalls s JOIN s.reviews r GROUP BY c.id ORDER BY AVG(r.rating) DESC")
    List<Cafeteria> findPopularCafeterias();

    @Query("SELECT c FROM Cafeteria c LEFT JOIN FETCH c.stalls WHERE c.id = :id")
    Optional<Cafeteria> findByIdWithStalls(@Param("id") Long id);
}