package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import java.util.UUID;

public record QuizQuestionResponse(
    UUID id,
    String questionText,
    String optionA,
    String optionB,
    String optionC,
    String optionD
) {
}
