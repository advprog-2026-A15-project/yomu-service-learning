package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import jakarta.validation.constraints.NotNull;
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
        private UUID questionId;
        private String selectedOption; // "A", "B", "C", or "D"
    }
}
