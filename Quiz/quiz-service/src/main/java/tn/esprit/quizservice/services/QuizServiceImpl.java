package tn.esprit.quizservice.services;

import tn.esprit.quizservice.dto.*;
import tn.esprit.quizservice.entities.AttemptAnswer;
import tn.esprit.quizservice.entities.Question;
import tn.esprit.quizservice.entities.QuestionOption;
import tn.esprit.quizservice.entities.Quiz;
import tn.esprit.quizservice.entities.QuizAttempt;
import tn.esprit.quizservice.repositories.AttemptAnswerRepository;
import tn.esprit.quizservice.repositories.QuestionOptionRepository;
import tn.esprit.quizservice.repositories.QuestionRepository;
import tn.esprit.quizservice.repositories.QuizAttemptRepository;
import tn.esprit.quizservice.repositories.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements IQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;

    @Override
    public List<QuizDTO> getQuizzesByCourse(Long courseId) {
        return quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream()
                .map(q -> QuizDTO.builder()
                        .id(q.getId())
                        .courseId(q.getCourseId())
                        .title(q.getTitle())
                        .description(q.getDescription())
                        .timeLimitMinutes(q.getTimeLimitMinutes())
                        .passingScore(q.getPassingScore())
                        .maxAttempts(q.getMaxAttempts())
                        .createdAt(q.getCreatedAt())
                        .questionCount(questionRepository.findByQuizIdOrderByOrderIndexAsc(q.getId()).size())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public QuizDTO getQuizById(Long courseId, Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        return QuizDTO.builder()
                .id(quiz.getId())
                .courseId(quiz.getCourseId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .createdAt(quiz.getCreatedAt())
                .questionCount(questionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId()).size())
                .build();
    }

    @Override
    public QuizDTO createQuiz(Long courseId, QuizRequestDTO request) {
        Quiz quiz = Quiz.builder()
                .courseId(courseId)
                .title(request.getTitle())
                .description(request.getDescription())
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 70)
                .maxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 3)
                .build();
        Quiz saved = quizRepository.save(quiz);
        return QuizDTO.builder()
                .id(saved.getId())
                .courseId(saved.getCourseId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .timeLimitMinutes(saved.getTimeLimitMinutes())
                .passingScore(saved.getPassingScore())
                .maxAttempts(saved.getMaxAttempts())
                .createdAt(saved.getCreatedAt())
                .questionCount(0)
                .build();
    }

    @Override
    @Transactional
    public void deleteQuiz(Long courseId, Long quizId) {
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        for (Question q : questions) {
            optionRepository.deleteByQuestionId(q.getId());
        }
        questionRepository.deleteByQuizId(quizId);
        quizRepository.deleteById(quizId);
    }

    @Override
    public List<QuestionDTO> getQuestionsByQuiz(Long quizId) {
        return questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId).stream()
                .map(q -> QuestionDTO.builder()
                        .id(q.getId())
                        .quizId(q.getQuizId())
                        .questionText(q.getQuestionText())
                        .orderIndex(q.getOrderIndex())
                        .createdAt(q.getCreatedAt())
                        .options(optionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId()).stream()
                                .map(o -> QuestionOptionDTO.builder()
                                        .id(o.getId())
                                        .questionId(o.getQuestionId())
                                        .optionText(o.getOptionText())
                                        .isCorrect(o.getIsCorrect())
                                        .orderIndex(o.getOrderIndex())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionDTO addQuestion(Long quizId, QuestionRequestDTO request) {
        int nextIndex = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId).size();
        Question question = Question.builder()
                .quizId(quizId)
                .questionText(request.getQuestionText())
                .orderIndex(nextIndex)
                .build();
        Question saved = questionRepository.save(question);

        List<QuestionOption> options = IntStream.range(0, request.getOptions().size())
                .mapToObj(i -> {
                    QuestionRequestDTO.OptionRequest opt = request.getOptions().get(i);
                    return QuestionOption.builder()
                            .questionId(saved.getId())
                            .optionText(opt.getOptionText())
                            .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                            .orderIndex(i)
                            .build();
                })
                .collect(Collectors.toList());
        List<QuestionOption> savedOptions = optionRepository.saveAll(options);

        return QuestionDTO.builder()
                .id(saved.getId())
                .quizId(saved.getQuizId())
                .questionText(saved.getQuestionText())
                .orderIndex(saved.getOrderIndex())
                .createdAt(saved.getCreatedAt())
                .options(savedOptions.stream()
                        .map(o -> QuestionOptionDTO.builder()
                                .id(o.getId())
                                .questionId(o.getQuestionId())
                                .optionText(o.getOptionText())
                                .isCorrect(o.getIsCorrect())
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public void deleteQuestion(Long quizId, Long questionId) {
        optionRepository.deleteByQuestionId(questionId);
        questionRepository.deleteById(questionId);
    }

    @Override
    @Transactional
    public QuizResultDTO submitQuiz(Long quizId, Long studentId, QuizSubmissionDTO submission) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quizId);
        int maxAttempts = quiz.getMaxAttempts() != null ? quiz.getMaxAttempts() : 3;
        if (attemptsUsed >= maxAttempts) {
            throw new RuntimeException("Max attempts reached");
        }

        int correctAnswers = 0;
        List<Boolean> correctFlags = new ArrayList<>();
        for (QuizAnswerDTO answer : submission.getAnswers()) {
            boolean isCorrect = false;
            if (answer.getSelectedOptionId() != null) {
                QuestionOption option = optionRepository.findById(answer.getSelectedOptionId()).orElse(null);
                if (option != null) {
                    isCorrect = Boolean.TRUE.equals(option.getIsCorrect());
                }
            }
            if (isCorrect) correctAnswers++;
            correctFlags.add(isCorrect);
        }

        int totalQuestions = submission.getAnswers().size();
        double score = totalQuestions > 0 ? (correctAnswers * 100.0 / totalQuestions) : 0.0;
        boolean passed = score >= quiz.getPassingScore();

        QuizAttempt attempt = QuizAttempt.builder()
                .studentId(studentId)
                .quiz(quiz)
                .score(score)
                .passed(passed)
                .correctAnswers(correctAnswers)
                .totalQuestions(totalQuestions)
                .build();
        QuizAttempt savedAttempt = quizAttemptRepository.save(attempt);

        List<AttemptAnswer> attemptAnswers = new ArrayList<>();
        List<QuizAnswerDTO> answers = submission.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            QuizAnswerDTO answer = answers.get(i);
            attemptAnswers.add(AttemptAnswer.builder()
                    .attempt(savedAttempt)
                    .questionId(answer.getQuestionId())
                    .selectedOptionId(answer.getSelectedOptionId())
                    .isCorrect(correctFlags.get(i))
                    .build());
        }
        attemptAnswerRepository.saveAll(attemptAnswers);

        // Build per-question review with correct answers revealed
        List<QuestionReviewDTO> reviews = new ArrayList<>();
        for (QuizAnswerDTO answer : submission.getAnswers()) {
            Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
            if (question == null) continue;

            QuestionOption selectedOption = null;
            if (answer.getSelectedOptionId() != null) {
                selectedOption = optionRepository.findById(answer.getSelectedOptionId()).orElse(null);
            }

            List<QuestionOption> options = optionRepository.findByQuestionIdOrderByOrderIndexAsc(question.getId());
            QuestionOption correctOption = options.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                    .findFirst()
                    .orElse(null);

            reviews.add(QuestionReviewDTO.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .selectedOptionId(selectedOption != null ? selectedOption.getId() : null)
                    .selectedOptionText(selectedOption != null ? selectedOption.getOptionText() : "Not answered")
                    .selectedCorrect(selectedOption != null && Boolean.TRUE.equals(selectedOption.getIsCorrect()))
                    .correctOptionId(correctOption != null ? correctOption.getId() : null)
                    .correctOptionText(correctOption != null ? correctOption.getOptionText() : "Unknown")
                    .build());
        }

        int newAttemptsUsed = attemptsUsed + 1;
        return QuizResultDTO.builder()
                .attemptId(savedAttempt.getId())
                .score(score)
                .passed(passed)
                .correctAnswers(correctAnswers)
                .totalQuestions(totalQuestions)
                .passingScore(quiz.getPassingScore())
                .attemptsUsed(newAttemptsUsed)
                .attemptsRemaining(maxAttempts - newAttemptsUsed)
                .questionReviews(reviews)
                .build();
    }

    @Override
    public int countAttempts(Long quizId, Long studentId) {
        return quizAttemptRepository.countByStudentIdAndQuizId(studentId, quizId);
    }

    @Override
    public List<LeaderboardEntryDTO> getLeaderboard(Long quizId) {
        List<QuizAttempt> top5 = quizAttemptRepository.findTop5ByQuizIdOrderByScoreDescAttemptedAtAsc(quizId);
        List<LeaderboardEntryDTO> result = new ArrayList<>();
        for (int i = 0; i < top5.size(); i++) {
            QuizAttempt attempt = top5.get(i);
            String rankTitle;
            if (attempt.getScore() >= 90) rankTitle = "LEGEND";
            else if (attempt.getScore() >= 75) rankTitle = "CHAMPION";
            else if (attempt.getScore() >= 60) rankTitle = "FIGHTER";
            else rankTitle = "CHALLENGER";
            result.add(LeaderboardEntryDTO.builder()
                    .rank(i + 1)
                    .studentId(attempt.getStudentId())
                    .studentName("Player #" + attempt.getStudentId())
                    .score(attempt.getScore())
                    .rankTitle(rankTitle)
                    .attemptedAt(attempt.getAttemptedAt())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public QuizDTO updateQuiz(Long quizId, QuizRequestDTO dto) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        if (dto.getPassingScore() != null) quiz.setPassingScore(dto.getPassingScore());
        quiz.setTimeLimitMinutes(dto.getTimeLimitMinutes());
        if (dto.getMaxAttempts() != null) quiz.setMaxAttempts(dto.getMaxAttempts());
        Quiz saved = quizRepository.save(quiz);
        return QuizDTO.builder()
                .id(saved.getId())
                .courseId(saved.getCourseId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .timeLimitMinutes(saved.getTimeLimitMinutes())
                .passingScore(saved.getPassingScore())
                .createdAt(saved.getCreatedAt())
                .questionCount(questionRepository.findByQuizIdOrderByOrderIndexAsc(saved.getId()).size())
                .build();
    }

    @Override
    @Transactional
    public QuestionDTO updateQuestion(Long questionId, QuestionRequestDTO dto) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        question.setQuestionText(dto.getQuestionText());
        Question saved = questionRepository.save(question);

        optionRepository.deleteByQuestionId(questionId);

        List<QuestionOption> newOptions = IntStream.range(0, dto.getOptions().size())
                .mapToObj(i -> {
                    QuestionRequestDTO.OptionRequest opt = dto.getOptions().get(i);
                    return QuestionOption.builder()
                            .questionId(saved.getId())
                            .optionText(opt.getOptionText())
                            .isCorrect(Boolean.TRUE.equals(opt.getIsCorrect()))
                            .orderIndex(i)
                            .build();
                })
                .collect(Collectors.toList());
        List<QuestionOption> savedOptions = optionRepository.saveAll(newOptions);

        return QuestionDTO.builder()
                .id(saved.getId())
                .quizId(saved.getQuizId())
                .questionText(saved.getQuestionText())
                .orderIndex(saved.getOrderIndex())
                .createdAt(saved.getCreatedAt())
                .options(savedOptions.stream()
                        .map(o -> QuestionOptionDTO.builder()
                                .id(o.getId())
                                .questionId(o.getQuestionId())
                                .optionText(o.getOptionText())
                                .isCorrect(o.getIsCorrect())
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public Double getBestScoreForStudentAndCourse(Long studentId, Long courseId) {
        return quizAttemptRepository
                .findBestScoreByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);
    }
}
