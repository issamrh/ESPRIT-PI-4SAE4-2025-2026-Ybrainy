package com.ybrainy.backend.repository;

import com.ybrainy.backend.entity.PackCategory;
import com.ybrainy.backend.entity.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackCategoryRepository extends JpaRepository<PackCategory, Long> {

    List<PackCategory> findByStatus(CategoryStatus status);

    Optional<PackCategory> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

