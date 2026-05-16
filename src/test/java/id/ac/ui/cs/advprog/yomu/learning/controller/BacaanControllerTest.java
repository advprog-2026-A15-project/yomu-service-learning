package id.ac.ui.cs.advprog.yomu.learning.controller;

import id.ac.ui.cs.advprog.yomu.learning.internal.controller.BacaanController;
import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomu.learning.internal.service.BacaanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BacaanControllerTest {

    @Mock
    BacaanService bacaanService;

    @InjectMocks
    BacaanController controller;

    UUID bacaanId;
    UUID userId;
    Bacaan sampleBacaan;

    @BeforeEach
    void setUp() {
        bacaanId = UUID.randomUUID();
        userId = UUID.randomUUID();
        sampleBacaan = Bacaan.builder()
                .id(bacaanId)
                .title("Test Bacaan")
                .content("Isi konten")
                .category("News")
                .createdByUserId("admin-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── listBacaan ──────────────────────────────────────────────────

    @Test
    void listBacaan_noCategory_returnsAll() {
        when(bacaanService.listBacaan(null)).thenReturn(List.of(sampleBacaan));

        List<Bacaan> result = controller.listBacaan(null);

        assertThat(result).hasSize(1);
        verify(bacaanService).listBacaan(null);
    }

    @Test
    void listBacaan_withCategory_returnsFiltered() {
        when(bacaanService.listBacaan("News")).thenReturn(List.of(sampleBacaan));

        List<Bacaan> result = controller.listBacaan("News");

        assertThat(result).hasSize(1);
        verify(bacaanService).listBacaan("News");
    }

    // ─── getBacaan ───────────────────────────────────────────────────

    @Test
    void getBacaan_found() {
        when(bacaanService.getBacaanById(bacaanId)).thenReturn(sampleBacaan);

        Bacaan result = controller.getBacaan(bacaanId);

        assertThat(result.getId()).isEqualTo(bacaanId);
    }

    @Test
    void getBacaan_notFound_throws404() {
        when(bacaanService.getBacaanById(any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

        assertThatThrownBy(() -> controller.getBacaan(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ─── createBacaan ────────────────────────────────────────────────

    @Test
    void createBacaan_withAuth_returns201() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        req.setTitle("Baru");
        req.setContent("isi");
        req.setCategory("Sport");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("admin-id");
        when(bacaanService.createBacaan(req, "admin-id")).thenReturn(sampleBacaan);

        ResponseEntity<Bacaan> response = controller.createBacaan(req, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sampleBacaan);
    }

    @Test
    void createBacaan_nullAuth_usesSystem() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        req.setTitle("Baru");
        req.setContent("isi");
        req.setCategory("Sport");

        when(bacaanService.createBacaan(eq(req), eq("system"))).thenReturn(sampleBacaan);

        ResponseEntity<Bacaan> response = controller.createBacaan(req, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(bacaanService).createBacaan(eq(req), eq("system"));
    }

    // ─── updateBacaan ────────────────────────────────────────────────

    @Test
    void updateBacaan_returns200() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        req.setTitle("Updated");
        req.setContent("isi baru");
        req.setCategory("Sport");

        when(bacaanService.updateBacaan(bacaanId, req)).thenReturn(sampleBacaan);

        ResponseEntity<Bacaan> response = controller.updateBacaan(bacaanId, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateBacaan_notFound_throws404() {
        CreateBacaanRequest req = new CreateBacaanRequest();
        when(bacaanService.updateBacaan(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

        assertThatThrownBy(() -> controller.updateBacaan(UUID.randomUUID(), req))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ─── deleteBacaan ────────────────────────────────────────────────

    @Test
    void deleteBacaan_returns204() {
        doNothing().when(bacaanService).deleteBacaan(bacaanId);

        ResponseEntity<Void> response = controller.deleteBacaan(bacaanId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteBacaan_notFound_throws404() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"))
                .when(bacaanService).deleteBacaan(any());

        assertThatThrownBy(() -> controller.deleteBacaan(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ─── addQuestion ─────────────────────────────────────────────────

    @Test
    void addQuestion_returns201() {
        CreateQuestionRequest req = new CreateQuestionRequest();
        req.setBacaanId(bacaanId);
        req.setQuestionText("Apa?");
        req.setOptionA("A");

        Question q = Question.builder().id(UUID.randomUUID()).bacaanId(bacaanId)
                .questionText("Apa?").correctOption("A").createdAt(LocalDateTime.now()).build();

        when(bacaanService.addQuestion(req)).thenReturn(q);

        ResponseEntity<Question> response = controller.addQuestion(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getQuestionText()).isEqualTo("Apa?");
    }

    // ─── getQuestions ────────────────────────────────────────────────

    @Test
    void getQuestions_returns200() {
        Question q = Question.builder().id(UUID.randomUUID()).bacaanId(bacaanId)
                .questionText("Q?").correctOption("B").createdAt(LocalDateTime.now()).build();

        when(bacaanService.getQuestionsByBacaanId(bacaanId)).thenReturn(List.of(q));

        List<QuizQuestionResponse> result = controller.getQuestions(bacaanId);

        assertThat(result).hasSize(1);
    }

    // ─── deleteQuestion ──────────────────────────────────────────────

    @Test
    void deleteQuestion_returns204() {
        UUID questionId = UUID.randomUUID();
        doNothing().when(bacaanService).deleteQuestion(questionId);

        ResponseEntity<Void> response = controller.deleteQuestion(questionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ─── submitQuiz ──────────────────────────────────────────────────

    @Test
    void submitQuiz_returns201() {
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);
        req.setAnswers(List.of());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        when(auth.getAuthorities()).thenReturn(List.of());

        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID()).userId(userId).bacaanId(bacaanId)
                .score(3).totalQuestions(5).completedAt(LocalDateTime.now()).build();

        when(bacaanService.submitQuiz(bacaanId, req)).thenReturn(attempt);

        ResponseEntity<QuizAttempt> response = controller.submitQuiz(bacaanId, req, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getScore()).isEqualTo(3);
    }

    @Test
    void submitQuiz_alreadyDone_throws409() {
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.setUserId(userId);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());
        when(auth.getAuthorities()).thenReturn(List.of());

        when(bacaanService.submitQuiz(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "sudah menyelesaikan kuis"));

        assertThatThrownBy(() -> controller.submitQuiz(bacaanId, req, auth))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ─── checkQuizStatus ─────────────────────────────────────────────

    @Test
    void checkQuizStatus_returnsTrue() {
        when(bacaanService.hasCompletedQuiz(userId, bacaanId)).thenReturn(true);

        ResponseEntity<Boolean> response = controller.checkQuizStatus(bacaanId, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void checkQuizStatus_returnsFalse() {
        when(bacaanService.hasCompletedQuiz(userId, bacaanId)).thenReturn(false);

        ResponseEntity<Boolean> response = controller.checkQuizStatus(bacaanId, userId);

        assertThat(response.getBody()).isFalse();
    }

    // ─── getUserStats ────────────────────────────────────────────────

    @Test
    void getUserStats_returns200() {
        QuizStatsResponse stats = QuizStatsResponse.builder()
                .userId(userId).quizCompleted(5).totalCorrect(8)
                .totalAnswered(10).accuracyPercent(80.0).recentAttempts(List.of()).build();

        when(bacaanService.getUserStats(userId)).thenReturn(stats);

        ResponseEntity<QuizStatsResponse> response = controller.getUserStats(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAccuracyPercent()).isEqualTo(80.0);
    }
}
