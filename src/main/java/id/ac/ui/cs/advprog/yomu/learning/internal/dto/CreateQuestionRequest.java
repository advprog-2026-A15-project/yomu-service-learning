package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateQuestionRequest {
    @NotNull(message = "Bacaan ID wajib diisi")
    private UUID bacaanId;

    @NotBlank(message = "Pertanyaan tidak boleh kosong")
    private String questionText;

    @NotBlank(message = "Opsi A tidak boleh kosong")
    private String optionA;

    @NotBlank(message = "Opsi B tidak boleh kosong")
    private String optionB;

    @NotBlank(message = "Opsi C tidak boleh kosong")
    private String optionC;

    @NotBlank(message = "Opsi D tidak boleh kosong")
    private String optionD;

    @NotBlank(message = "Jawaban benar wajib diisi (A/B/C/D)")
    private String correctOption;
}
