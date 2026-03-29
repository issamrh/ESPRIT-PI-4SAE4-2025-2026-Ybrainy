package tn.esprit.quizservice.repositories;

import tn.esprit.quizservice.entities.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    int countByStudentIdAndQuizId(Long studentId, Long quizId);
    List<QuizAttempt> findByQuizIdIn(List<Long> quizIds);
    void deleteAllByQuizIdIn(List<Long> quizIds);
    List<QuizAttempt> findTop5ByQuizIdOrderByScoreDescAttemptedAtAsc(Long quizId);

    @Query("SELECT a FROM QuizAttempt a WHERE a.studentId = :studentId AND a.quiz.id IN :quizIds ORDER BY a.score DESC")
    List<QuizAttempt> findByStudentIdAndQuizIds(@Param("studentId") Long studentId, @Param("quizIds") List<Long> quizIds);
}
