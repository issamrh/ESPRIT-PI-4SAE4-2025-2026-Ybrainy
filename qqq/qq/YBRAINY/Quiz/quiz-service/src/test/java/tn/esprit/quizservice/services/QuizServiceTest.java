package tn.esprit.quizservice.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.quizservice.entities.*;
import tn.esprit.quizservice.repositories.*;
import tn.esprit.quizservice.clients.*;
import tn.esprit.quizservice.dto.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizRepository quizRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository optionRepository;
    @Mock QuizAttemptRepository attemptRepository;
    @Mock AttemptAnswerRepository answerRepository;
    @Mock CourseClient courseClient;
    @Mock EnrollmentClient enrollmentClient;
    @Mock UserClient userClient;
    @InjectMocks QuizServiceImpl service;

    private Quiz sampleQuiz() {
        Quiz q = new Quiz();
        q.setId(1L);
        q.setCourseId(100L);
        q.setTitle("Test Quiz");
        q.setPassingScore(70);
        q.setMaxAttempts(3);
        q.setTimeLimitMinutes(30);
        q.setIsActive(true);
        return q;
    }

    @Test
    @DisplayName("getQuizzesByCourse returns quizzes for the course")
    void getQuizzesByCourse_returnsList() {
        when(quizRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(sampleQuiz()));
        when(questionRepository.findByQuizIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        var result = service.getQuizzesByCourse(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCourseId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("createQuiz validates course exists via Feign")
    void createQuiz_invalidCourse_throws() {
        when(courseClient.courseExists(999L)).thenReturn(false);

        var request = new QuizRequestDTO();
        request.setTitle("Bad Quiz");

        assertThatThrownBy(() -> service.createQuiz(999L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Score is 100% when all answers are correct")
    void submitQuiz_allCorrect_score100() {
        Quiz quiz = sampleQuiz();

        Question q1 = new Question();
        q1.setId(1L);
        q1.setQuizId(quiz.getId());
        q1.setPoints(10);

        QuestionOption correct = new QuestionOption();
        correct.setId(1L);
        correct.setQuestionId(q1.getId());
        correct.setIsCorrect(true);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(1L);
        attempt.setStudentId(10L);
        attempt.setQuiz(quiz);
        attempt.setScore(100.0);
        attempt.setPassed(true);
        attempt.setCorrectAnswers(1);
        attempt.setTotalQuestions(1);

        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(enrollmentClient.isEnrolled(10L, 100L)).thenReturn(true);
        when(attemptRepository.countByStudentIdAndQuizId(10L, 1L)).thenReturn(0);
        when(attemptRepository.save(any())).thenReturn(attempt);
        when(answerRepository.saveAll(any())).thenReturn(List.of());
        when(optionRepository.findById(1L)).thenReturn(Optional.of(correct));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(optionRepository.findByQuestionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(correct));

        var submission = new QuizSubmissionDTO();
        var answer = new QuizAnswerDTO();
        answer.setQuestionId(1L);
        answer.setSelectedOptionId(1L);
        submission.setAnswers(List.of(answer));

        var result = service.submitQuiz(1L, 10L, submission);

        assertThat(result.getScore()).isEqualTo(100.0);
        assertThat(result.getPassed()).isTrue();
    }

    @Test
    @DisplayName("Score is 0% when all answers are wrong")
    void submitQuiz_allWrong_score0() {
        Quiz quiz = sampleQuiz();

        Question q1 = new Question();
        q1.setId(1L);
        q1.setQuizId(quiz.getId());
        q1.setPoints(10);

        QuestionOption wrong = new QuestionOption();
        wrong.setId(2L);
        wrong.setQuestionId(q1.getId());
        wrong.setIsCorrect(false);

        QuestionOption correct = new QuestionOption();
        correct.setId(1L);
        correct.setQuestionId(q1.getId());
        correct.setIsCorrect(true);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId(1L);
        attempt.setStudentId(10L);
        attempt.setQuiz(quiz);
        attempt.setScore(0.0);
        attempt.setPassed(false);
        attempt.setCorrectAnswers(0);
        attempt.setTotalQuestions(1);

        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(enrollmentClient.isEnrolled(10L, 100L)).thenReturn(true);
        when(attemptRepository.countByStudentIdAndQuizId(10L, 1L)).thenReturn(0);
        when(attemptRepository.save(any())).thenReturn(attempt);
        when(answerRepository.saveAll(any())).thenReturn(List.of());
        when(optionRepository.findById(2L)).thenReturn(Optional.of(wrong));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(q1));
        when(optionRepository.findByQuestionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(correct, wrong));

        var submission = new QuizSubmissionDTO();
        var answer = new QuizAnswerDTO();
        answer.setQuestionId(1L);
        answer.setSelectedOptionId(2L); // wrong option
        submission.setAnswers(List.of(answer));

        var result = service.submitQuiz(1L, 10L, submission);

        assertThat(result.getScore()).isEqualTo(0.0);
        assertThat(result.getPassed()).isFalse();
    }

    @Test
    @DisplayName("Submitting quiz beyond maxAttempts throws exception")
    void submitQuiz_maxAttemptsExceeded_throws() {
        Quiz quiz = sampleQuiz(); // maxAttempts = 3

        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(enrollmentClient.isEnrolled(10L, 100L)).thenReturn(true);
        when(attemptRepository.countByStudentIdAndQuizId(10L, 1L)).thenReturn(3);

        var submission = new QuizSubmissionDTO();
        submission.setAnswers(List.of());

        assertThatThrownBy(() -> service.submitQuiz(1L, 10L, submission))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("attempt");
    }

    @Test
    @DisplayName("Non-enrolled student cannot submit quiz")
    void submitQuiz_notEnrolled_throws() {
        Quiz quiz = sampleQuiz();
        when(quizRepository.findById(1L)).thenReturn(Optional.of(quiz));
        when(enrollmentClient.isEnrolled(10L, 100L)).thenReturn(false);

        var submission = new QuizSubmissionDTO();
        submission.setAnswers(List.of());

        assertThatThrownBy(() -> service.submitQuiz(1L, 10L, submission))
            .isInstanceOf(RuntimeException.class);
    }
}
