package id.ac.ui.cs.advprog.yomu.learning.internal.controller;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.service.BacaanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller untuk modul Bacaan & Kuis.
 * Admin: CRUD bacaan dan soal kuis.
 * Pelajar: Lihat daftar bacaan, baca teks, kerjakan kuis.
 */
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class BacaanController {

    private final BacaanService bacaanService;

    // ─── Bacaan Endpoints ────────────────────────────────────────────

    /** Admin: Buat bacaan baru */
    @PostMapping("/bacaan")
    public ResponseEntity<Bacaan> createBacaan(
            @Valid @RequestBody CreateBacaanRequest request,
            Authentication auth) {
        String adminUserId = auth != null ? (String) auth.getCredentials() : "system";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bacaanService.createBacaan(request, adminUserId));
    }

    /** Pelajar/Admin: Lihat daftar bacaan */
    @GetMapping("/bacaan")
    public List<Bacaan> listBacaan(@RequestParam(required = false) String category) {
        return bacaanService.listBacaan(category);
    }

    /** Pelajar: Lihat detail bacaan */
    @GetMapping("/bacaan/{id}")
    public Bacaan getBacaan(@PathVariable UUID id) {
        return bacaanService.getBacaanById(id);
    }

    /** Admin: Hapus bacaan */
    @DeleteMapping("/bacaan/{id}")
    public ResponseEntity<Void> deleteBacaan(@PathVariable UUID id) {
        bacaanService.deleteBacaan(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Question Endpoints ──────────────────────────────────────────

    /** Admin: Tambah pertanyaan kuis */
    @PostMapping("/questions")
    public ResponseEntity<Question> addQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bacaanService.addQuestion(request));
    }

    /** Pelajar: Lihat soal kuis untuk bacaan tertentu */
    @GetMapping("/bacaan/{bacaanId}/questions")
    public List<Question> getQuestions(@PathVariable UUID bacaanId) {
        return bacaanService.getQuestionsByBacaanId(bacaanId);
    }

    /** Admin: Hapus pertanyaan */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID questionId) {
        bacaanService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    // ─── Quiz Endpoints ──────────────────────────────────────────────

    /** Pelajar: Submit jawaban kuis */
    @PostMapping("/bacaan/{bacaanId}/quiz")
    public ResponseEntity<QuizAttempt> submitQuiz(
            @PathVariable UUID bacaanId,
            @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bacaanService.submitQuiz(bacaanId, request));
    }

    /** Pelajar: Cek apakah sudah mengerjakan kuis */
    @GetMapping("/bacaan/{bacaanId}/quiz/status")
    public ResponseEntity<Boolean> checkQuizStatus(
            @PathVariable UUID bacaanId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(bacaanService.hasCompletedQuiz(userId, bacaanId));
    }
}
