package id.ac.ui.cs.advprog.yomu.learning.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model untuk pertanyaan kuis yang terkait dengan sebuah bacaan.
 * Setiap pertanyaan memiliki 4 pilihan jawaban (A-D) dengan satu jawaban benar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private UUID id;
    private UUID bacaanId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption; // "A", "B", "C", or "D"
    private LocalDateTime createdAt;
}
