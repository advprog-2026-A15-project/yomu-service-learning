package id.ac.ui.cs.advprog.yomu.learning.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model untuk teks bacaan yang akan dibaca oleh pelajar.
 * Bacaan dibagi ke dalam kategori seperti "News & Media", "Olahraga", dll.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bacaan {
    private UUID id;
    private String title;
    private String content;
    private String category;
    private String createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
