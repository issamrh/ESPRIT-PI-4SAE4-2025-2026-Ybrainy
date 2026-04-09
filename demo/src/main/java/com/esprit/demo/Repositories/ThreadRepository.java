package com.esprit.demo.Repositories;

import com.esprit.demo.Models.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.esprit.demo.Models.Thread;


import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<Thread, Long> {

    java.util.Optional<Thread> findById(Long id);

    List<Thread> findByCategoryId(Long categoryId);

    List<Thread> findByAuthorId(Long authorId);

    List<Thread> findByStatus(Status status);

    List<Thread> findByCategoryIdAndStatus(Long categoryId, Status status);

    @Query("SELECT t FROM Thread t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%',:kw,'%')) OR LOWER(t.body) LIKE LOWER(CONCAT('%',:kw,'%'))")
    List<Thread> searchByKeyword(@Param("kw") String kw, Pageable pageable);
}
