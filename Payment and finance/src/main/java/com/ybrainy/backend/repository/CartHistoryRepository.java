package com.ybrainy.backend.repository;

import com.ybrainy.backend.entity.CartHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartHistoryRepository extends JpaRepository<CartHistory, Long> {
    List<CartHistory> findByUserId(Long userId);
}
