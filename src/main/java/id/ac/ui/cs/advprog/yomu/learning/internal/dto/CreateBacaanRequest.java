package id.ac.ui.cs.advprog.yomu.learning.internal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBacaanRequest {
    @NotBlank(message = "Judul bacaan tidak boleh kosong")
    private String title;

    @NotBlank(message = "Konten bacaan tidak boleh kosong")
    private String content;

    @NotBlank(message = "Kategori tidak boleh kosong")
    private String category;
}
