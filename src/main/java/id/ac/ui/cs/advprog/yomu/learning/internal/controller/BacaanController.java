package id.ac.ui.cs.advprog.yomu.learning.internal.controller;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.service.BacaanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/bacaan")
    public ResponseEntity<Bacaan> createBacaan(
            @Valid @RequestBody CreateBacaanRequest request,
            Authentication auth) {
        String adminUserId = auth != null ? auth.getPrincipal().toString() : "system";
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

    /** Admin: Edit bacaan */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/bacaan/{id}")
    public ResponseEntity<Bacaan> updateBacaan(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBacaanRequest request) {
        return ResponseEntity.ok(bacaanService.updateBacaan(id, request));
    }

    /** Admin: Hapus bacaan */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/bacaan/{id}")
    public ResponseEntity<Void> deleteBacaan(@PathVariable UUID id) {
        bacaanService.deleteBacaan(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Question Endpoints ──────────────────────────────────────────

    /** Admin: Tambah pertanyaan kuis */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/questions")
    public ResponseEntity<Question> addQuestion(@Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bacaanService.addQuestion(request));
    }

    /** Pelajar: Lihat soal kuis untuk bacaan tertentu (tanpa jawaban benar) */
    @GetMapping("/bacaan/{bacaanId}/questions")
    public List<QuizQuestionResponse> getQuestions(@PathVariable UUID bacaanId) {
        return bacaanService.getQuestionsByBacaanId(bacaanId).stream()
                .map(question -> new QuizQuestionResponse(
                    question.getId(),
                    question.getQuestionText(),
                    question.getOptionA(),
                    question.getOptionB(),
                    question.getOptionC(),
                    question.getOptionD()
                ))
                .toList();
    }

    /** Admin: Hapus pertanyaan */
    @PreAuthorize("hasRole('ADMIN')")
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
            @Valid @RequestBody SubmitQuizRequest request,
            Authentication auth) {
        validateQuizOwner(request.getUserId(), auth);
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

    // ─── Stats Endpoints (For Liga Integration) ──────────────────────

    /** Liga: Ambil statistik kuis user */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<QuizStatsResponse> getUserStats(@PathVariable UUID userId) {
        return ResponseEntity.ok(bacaanService.getUserStats(userId));
    }

    // ─── Private Helpers ─────────────────────────────────────────────

    private void validateQuizOwner(UUID requestUserId, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User tidak terautentikasi");
        }

        boolean admin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
        if (admin) {
            return;
        }

        UUID authenticatedUserId = UUID.fromString(auth.getPrincipal().toString());
        if (!authenticatedUserId.equals(requestUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "User tidak dapat submit kuis untuk akun lain");
        }
    }

    private UUID resolveTargetUserId(UUID requestedUserId, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            if (requestedUserId != null) {
                return requestedUserId;
            }
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "User tidak terautentikasi");       
        }

        boolean admin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
        UUID authenticatedUserId = UUID.fromString(auth.getPrincipal().toString());
        if (requestedUserId == null || authenticatedUserId.equals(requestedUserId) || admin) {
            return requestedUserId == null ? authenticatedUserId : requestedUserId;
        }

        throw new org.springframework.web.server.ResponseStatusException(    
            HttpStatus.FORBIDDEN, "User tidak dapat mengakses status kuis akun lain");
    }}
