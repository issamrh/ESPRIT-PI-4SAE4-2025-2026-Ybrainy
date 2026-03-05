package com.esprit.demo.Repositories;

import com.esprit.demo.Models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
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


}
