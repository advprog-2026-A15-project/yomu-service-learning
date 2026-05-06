package id.ac.ui.cs.advprog.yomu.learning.internal.service;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;

import java.util.List;
import java.util.UUID;

/**
 * Interface BacaanService — Abstraksi untuk business logic modul Bacaan & Kuis.
 * Mengikuti Interface Segregation Principle (ISP): hanya method yang relevan.
 */
public interface BacaanService {

    // ─── Bacaan (CRUD by Admin) ──────────────────────────────────────

    Bacaan createBacaan(CreateBacaanRequest request, String adminUserId);

    List<Bacaan> listBacaan(String category);

    Bacaan getBacaanById(UUID id);

    void deleteBacaan(UUID id);

    // ─── Questions (CRUD by Admin) ───────────────────────────────────

    Question addQuestion(CreateQuestionRequest request);

    List<Question> getQuestionsByBacaanId(UUID bacaanId);

    void deleteQuestion(UUID questionId);

    // ─── Quiz (By Pelajar) ───────────────────────────────────────────

    QuizAttempt submitQuiz(UUID bacaanId, SubmitQuizRequest request);

    boolean hasCompletedQuiz(UUID userId, UUID bacaanId);
}
