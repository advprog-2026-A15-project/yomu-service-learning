package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request untuk mengerjakan kuis: berisi jawaban pelajar.
 */
@Data
public class SubmitQuizRequest {
    @NotNull(message = "User ID wajib diisi")
    private UUID userId;

    @NotNull(message = "Jawaban wajib diisi")
    private List<AnswerEntry> answers;

    @Data
    public static class AnswerEntry {
        @NotNull(message = "Question ID wajib diisi")
        private UUID questionId;

        @NotBlank(message = "Jawaban wajib diisi")
        @Pattern(regexp = "[A-Da-d]", message = "Pilihan jawaban harus A, B, C, atau D")
        private String selectedOption; // "A", "B", "C", or "D"
    }
}
