package com.ybrainy.backend.repository;

import com.ybrainy.backend.entity.CvSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CvSubmissionRepository extends JpaRepository<CvSubmission, Long> {
    List<CvSubmission> findAllByOrderBySubmittedAtDesc();
    List<CvSubmission> findByStatusOrderBySubmittedAtDesc(String status);
    List<CvSubmission> findByPositionContainingIgnoreCaseOrderBySubmittedAtDesc(String position);
    int countByStatus(String status);
}

