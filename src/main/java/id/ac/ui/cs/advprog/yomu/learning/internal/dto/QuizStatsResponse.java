package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import id.ac.ui.cs.advprog.yomu.learning.internal.model.QuizAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatsResponse {
    private UUID userId;
    private long quizCompleted;
    private long totalCorrect;
    private long totalAnswered;
    private double accuracyPercent;
    private List<QuizAttempt> recentAttempts;
}
