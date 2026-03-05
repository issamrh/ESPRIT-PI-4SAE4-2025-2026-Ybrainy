package com.esprit.demo.Repositories;

import com.esprit.demo.Models.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"author", "thread"})
    Optional<Post> findById(Long id);

    @EntityGraph(attributePaths = {"author", "thread"})
    List<Post> findByThreadId(Long threadId);

    @EntityGraph(attributePaths = {"author", "thread"})
    List<Post> findByAuthorId(Long authorId);

    long countByThreadId(Long threadId);

    /** Deletes all posts belonging to a given thread */
    @Modifying
    @Query("DELETE FROM Post p WHERE p.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);
}
