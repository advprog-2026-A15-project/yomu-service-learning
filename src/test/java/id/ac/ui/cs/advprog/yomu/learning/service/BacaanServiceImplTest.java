package id.ac.ui.cs.advprog.yomu.learning.service;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.repository.BacaanRepository;
import id.ac.ui.cs.advprog.yomu.learning.internal.service.BacaanServiceImpl;
import id.ac.ui.cs.advprog.yomu.shared.event.LearningCompletedEvent;
import id.ac.ui.cs.advprog.yomu.shared.event.QuizCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BacaanServiceImplTest {

    @Mock
    BacaanRepository repository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @InjectMocks
    BacaanServiceImpl service;

    UUID bacaanId;
    UUID userId;
    Bacaan sampleBacaan;

    @BeforeEach
    void setUp() {
        bacaanId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sampleBacaan = Bacaan.builder()
                .id(bacaanId)
                .title("Judul")
                .content("Isi bacaan")
                .category("News")
                .createdByUserId("admin-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createBacaan ────────────────────────────────────────────────

    @Test
    void createBacaan_success() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        req.setTitle("Judul Baru");
        req.setContent("Isi baru");
        req.setCategory("Sport");

        when(repository.saveBacaan(any())).thenAnswer(inv -> inv.getArgument(0));

        Bacaan result = service.createBacaan(req, "admin-1");

        assertThat(result.getTitle()).isEqualTo("Judul Baru");
        assertThat(result.getCategory()).isEqualTo("Sport");
        assertThat(result.getCreatedByUserId()).isEqualTo("admin-1");
        assertThat(result.getId()).isNotNull();
        verify(repository).saveBacaan(any());
    }

    // ─── listBacaan ──────────────────────────────────────────────────

    @Test
    void listBacaan_noCategory_returnsAll() {
        when(repository.findAllBacaan()).thenReturn(List.of(sampleBacaan));

        List<Bacaan> result = service.listBacaan(null);

        assertThat(result).hasSize(1);
        verify(repository).findAllBacaan();
        verify(repository, never()).findBacaanByCategory(any());
    }

    @Test
    void listBacaan_emptyCategory_returnsAll() {
        when(repository.findAllBacaan()).thenReturn(List.of(sampleBacaan));

        List<Bacaan> result = service.listBacaan("  ");

        assertThat(result).hasSize(1);
        verify(repository).findAllBacaan();
    }

    @Test
    void listBacaan_withCategory_returnsFiltered() {
        when(repository.findBacaanByCategory("News")).thenReturn(List.of(sampleBacaan));

        List<Bacaan> result = service.listBacaan("News");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("News");
        verify(repository).findBacaanByCategory("News");
        verify(repository, never()).findAllBacaan();
    }

    // ─── getBacaanById ───────────────────────────────────────────────

    @Test
    void getBacaanById_found() {
        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));

        Bacaan result = service.getBacaanById(bacaanId);

        assertThat(result.getId()).isEqualTo(bacaanId);
    }

    @Test
    void getBacaanById_notFound_throws404() {
        when(repository.findBacaanById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBacaanById(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bacaan tidak ditemukan");
    }

    // ─── updateBacaan ────────────────────────────────────────────────

    @Test
    void updateBacaan_success() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        req.setTitle("Updated");
        req.setContent("Isi baru");
        req.setCategory("Sport");

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.saveBacaan(any())).thenAnswer(inv -> inv.getArgument(0));

        Bacaan result = service.updateBacaan(bacaanId, req);

        assertThat(result.getTitle()).isEqualTo("Updated");
        assertThat(result.getCategory()).isEqualTo("Sport");
    }

    @Test
    void updateBacaan_notFound_throws404() {
        when(repository.findBacaanById(any())).thenReturn(Optional.empty());
        CreateBacaanRequest req = new CreateBacaanRequest();

        assertThatThrownBy(() -> service.updateBacaan(UUID.randomUUID(), req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bacaan tidak ditemukan");
    }

    // ─── deleteBacaan ────────────────────────────────────────────────

    @Test
    void deleteBacaan_success() {
        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));

        service.deleteBacaan(bacaanId);

        verify(repository).deleteBacaanById(bacaanId);
    }

    @Test
    void deleteBacaan_notFound_throws404() {
        when(repository.findBacaanById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteBacaan(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bacaan tidak ditemukan");
    }

    // ─── addQuestion ─────────────────────────────────────────────────

    @Test
    void addQuestion_success() {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setBacaanId(bacaanId);
        req.setQuestionText("Apa ini?");
        req.setOptionA("A");
        req.setOptionB("B");
        req.setOptionC("C");
        req.setOptionD("D");
        req.setCorrectOption("a");

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.saveQuestion(any())).thenAnswer(inv -> inv.getArgument(0));

        Question result = service.addQuestion(req);

        assertThat(result.getCorrectOption()).isEqualTo("A");
        assertThat(result.getBacaanId()).isEqualTo(bacaanId);
        verify(repository).saveQuestion(any());
    }

    @Test
    void addQuestion_bacaanNotFound_throws404() {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setBacaanId(UUID.randomUUID());

        when(repository.findBacaanById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addQuestion(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bacaan tidak ditemukan");
    }

    // ─── getQuestionsByBacaanId ──────────────────────────────────────

    @Test
    void getQuestionsByBacaanId_returnsQuestions() {
        Question q = Question.builder().id(UUID.randomUUID()).bacaanId(bacaanId).build();
        when(repository.findQuestionsByBacaanId(bacaanId)).thenReturn(List.of(q));

        List<Question> result = service.getQuestionsByBacaanId(bacaanId);

        assertThat(result).hasSize(1);
    }

    // ─── deleteQuestion ──────────────────────────────────────────────

    @Test
    void deleteQuestion_callsRepository() {
        UUID questionId = UUID.randomUUID();
        service.deleteQuestion(questionId);
        verify(repository).deleteQuestionById(questionId);
    }

    // ─── submitQuiz ──────────────────────────────────────────────────

    @Test
    void submitQuiz_success_calculatesScoreCorrectly() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        Question q1 = Question.builder().id(q1Id).bacaanId(bacaanId).correctOption("A").build();
        Question q2 = Question.builder().id(q2Id).bacaanId(bacaanId).correctOption("B").build();

        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of(
                answerEntry(q1Id, "A"),
                answerEntry(q2Id, "C")
        ));

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(false);
        when(repository.findQuestionsByBacaanId(bacaanId)).thenReturn(List.of(q1, q2));
        when(repository.saveQuizAttempt(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttempt result = service.submitQuiz(bacaanId, req);

        assertThat(result.getScore()).isEqualTo(1);
        assertThat(result.getTotalQuestions()).isEqualTo(2);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getBacaanId()).isEqualTo(bacaanId);
    }

    @Test
    void submitQuiz_publishesEvents() {
        UUID q1Id = UUID.randomUUID();
        Question q1 = Question.builder().id(q1Id).bacaanId(bacaanId).correctOption("A").build();

        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of(answerEntry(q1Id, "A")));

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(false);
        when(repository.findQuestionsByBacaanId(bacaanId)).thenReturn(List.of(q1));
        when(repository.saveQuizAttempt(any())).thenAnswer(inv -> inv.getArgument(0));

        service.submitQuiz(bacaanId, req);

        verify(rabbitTemplate).convertAndSend(eq("yomu.learning.completed"), any(LearningCompletedEvent.class));
        verify(rabbitTemplate).convertAndSend(eq("yomu.quiz.completed"), any(QuizCompletedEvent.class));
    }

    @Test
    void submitQuiz_alreadyCompleted_throws409() {
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of());

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(true);

        assertThatThrownBy(() -> service.submitQuiz(bacaanId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sudah menyelesaikan kuis");
    }

    @Test
    void submitQuiz_bacaanNotFound_throws404() {
        when(repository.findBacaanById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitQuiz(UUID.randomUUID(), new SubmitQuizRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bacaan tidak ditemukan");
    }

    @Test
    void submitQuiz_noQuestions_throws400() {
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of());

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(false);
        when(repository.findQuestionsByBacaanId(bacaanId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.submitQuiz(bacaanId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Belum ada soal");
    }

    @Test
    void submitQuiz_allWrongAnswers_scoreZero() {
        UUID q1Id = UUID.randomUUID();
        Question q1 = Question.builder().id(q1Id).bacaanId(bacaanId).correctOption("A").build();

        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of(answerEntry(q1Id, "B")));

        when(repository.findBacaanById(bacaanId)).thenReturn(Optional.of(sampleBacaan));
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(false);
        when(repository.findQuestionsByBacaanId(bacaanId)).thenReturn(List.of(q1));
        when(repository.saveQuizAttempt(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizAttempt result = service.submitQuiz(bacaanId, req);

        assertThat(result.getScore()).isZero();
    }

    // ─── hasCompletedQuiz ────────────────────────────────────────────

    @Test
    void hasCompletedQuiz_returnsTrue() {
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(true);
        assertThat(service.hasCompletedQuiz(userId, bacaanId)).isTrue();
    }

    @Test
    void hasCompletedQuiz_returnsFalse() {
        when(repository.hasUserCompletedQuiz(userId, bacaanId)).thenReturn(false);
        assertThat(service.hasCompletedQuiz(userId, bacaanId)).isFalse();
    }

    // ─── getUserStats ────────────────────────────────────────────────

    @Test
    void getUserStats_calculatesAccuracyCorrectly() {
        BacaanRepository.QuizStats stats = new BacaanRepository.QuizStats(5, 8, 10);
        when(repository.getStatsByUserId(userId)).thenReturn(stats);
        when(repository.findAttemptsByUserId(userId)).thenReturn(List.of());

        QuizStatsResponse result = service.getUserStats(userId);

        assertThat(result.getQuizCompleted()).isEqualTo(5);
        assertThat(result.getTotalCorrect()).isEqualTo(8);
        assertThat(result.getTotalAnswered()).isEqualTo(10);
        assertThat(result.getAccuracyPercent()).isEqualTo(80.0);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void getUserStats_noAttempts_accuracyZero() {
        BacaanRepository.QuizStats stats = new BacaanRepository.QuizStats(0, 0, 0);
        when(repository.getStatsByUserId(userId)).thenReturn(stats);
        when(repository.findAttemptsByUserId(userId)).thenReturn(List.of());

        QuizStatsResponse result = service.getUserStats(userId);

        assertThat(result.getAccuracyPercent()).isZero();
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private SubmitQuizRequest.AnswerEntry answerEntry(UUID questionId, String option) {
        SubmitQuizRequest.AnswerEntry entry = new SubmitQuizRequest.AnswerEntry();
        entry.setQuestionId(questionId);
        entry.setSelectedOption(option);
        return entry;
    }
}
