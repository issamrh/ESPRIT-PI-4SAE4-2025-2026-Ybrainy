package com.esprit.demo.Repositories;

import com.esprit.demo.Models.ThreadWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreadWishlistRepository extends JpaRepository<ThreadWishlist, Long> {

    Optional<ThreadWishlist> findByUserIdAndThreadId(Long userId, Long threadId);

    List<ThreadWishlist> findByUserId(Long userId);

    boolean existsByUserIdAndThreadId(Long userId, Long threadId);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    @Modifying
    @Query("DELETE FROM ThreadWishlist w WHERE w.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);

    /** Count how many users saved threads authored by a given user */
    @Query("SELECT COUNT(w) FROM ThreadWishlist w WHERE w.thread.author.id = :authorId")
    long countByThreadAuthorId(@Param("authorId") Long authorId);
}
