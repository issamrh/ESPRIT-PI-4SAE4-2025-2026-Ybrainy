package com.esprit.demo.Repositories;

import com.esprit.demo.Models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);
    List<Comment> findByAuthorId(Long authorId);
    long countByPostId(Long postId);

    /** Deletes all comments belonging to a specific post */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    /** Deletes all comments whose post belongs to a given thread */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.thread.id = :threadId")
    void deleteByPostThreadId(@Param("threadId") Long threadId);
}
