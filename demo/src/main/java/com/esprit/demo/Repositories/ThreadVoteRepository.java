package com.esprit.demo.Repositories;

import com.esprit.demo.Models.ThreadVote;
import com.esprit.demo.Models.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ThreadVoteRepository extends JpaRepository<ThreadVote, Long> {

    Optional<ThreadVote> findByUserIdAndThreadId(Long userId, Long threadId);

    long countByThreadIdAndVoteType(Long threadId, VoteType voteType);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    @Modifying
    @Query("DELETE FROM ThreadVote v WHERE v.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);
}
