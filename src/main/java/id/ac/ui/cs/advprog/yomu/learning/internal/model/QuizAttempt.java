package id.ac.ui.cs.advprog.yomu.learning.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mencatat percobaan kuis oleh pelajar.
 * Pelajar yang sudah menyelesaikan kuis dari suatu teks tidak dapat mengerjakan kuis itu lagi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {
    private UUID id;
    private UUID userId;
    private UUID bacaanId;
    private int score;           // jumlah jawaban benar
    private int totalQuestions;  // total soal
    private LocalDateTime completedAt;
}
