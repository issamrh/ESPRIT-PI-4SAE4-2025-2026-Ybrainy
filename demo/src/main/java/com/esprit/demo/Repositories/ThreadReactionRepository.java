package com.esprit.demo.Repositories;

import com.esprit.demo.Models.ReactionType;
import com.esprit.demo.Models.ThreadReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ThreadReactionRepository extends JpaRepository<ThreadReaction, Long> {

    Optional<ThreadReaction> findByUserIdAndThreadId(Long userId, Long threadId);

    long countByThreadIdAndReactionType(Long threadId, ReactionType reactionType);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    @Modifying
    @Query("DELETE FROM ThreadReaction r WHERE r.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);
}
