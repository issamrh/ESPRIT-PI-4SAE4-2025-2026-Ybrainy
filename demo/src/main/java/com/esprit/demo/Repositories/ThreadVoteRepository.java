package com.esprit.demo.Repositories;

import com.esprit.demo.Models.ThreadVote;
import com.esprit.demo.Models.VoteType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThreadVoteRepository extends JpaRepository<ThreadVote, Long> {

    Optional<ThreadVote> findByUserIdAndThreadId(Long userId, Long threadId);

    long countByThreadIdAndVoteType(Long threadId, VoteType voteType);

    void deleteByUserIdAndThreadId(Long userId, Long threadId);

    @Modifying
    @Query("DELETE FROM ThreadVote v WHERE v.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") Long threadId);

    /** Count votes on threads authored by a specific user */
    @Query("SELECT COUNT(v) FROM ThreadVote v WHERE v.thread.author.id = :authorId AND v.voteType = :voteType")
    long countByThreadAuthorIdAndVoteType(@Param("authorId") Long authorId, @Param("voteType") VoteType voteType);

    /** Total votes of a given type across all threads (community avg) */
    long countByVoteType(VoteType voteType);

    /** Thread ids + upvote counts for an author (best thread detection) */
    @Query("SELECT v.thread.id, COUNT(v) AS cnt FROM ThreadVote v WHERE v.thread.author.id = :authorId AND v.voteType = 'UPVOTE' GROUP BY v.thread.id ORDER BY cnt DESC")
    List<Object[]> findUpvoteCountPerThreadForAuthor(@Param("authorId") Long authorId, Pageable pageable);
}
