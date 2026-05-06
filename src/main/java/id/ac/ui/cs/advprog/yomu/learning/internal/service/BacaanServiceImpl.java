package id.ac.ui.cs.advprog.yomu.learning.internal.service;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.repository.BacaanRepository;
import id.ac.ui.cs.advprog.yomu.shared.event.LearningCompletedEvent;
import id.ac.ui.cs.advprog.yomu.shared.event.QuizCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementasi BacaanService — menangani logika bisnis untuk bacaan dan kuis.
 * Mematuhi prinsip OCP: menambah event baru tidak memerlukan perubahan di service lain.
 */
@Service
public class BacaanServiceImpl implements BacaanService {

    private final BacaanRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public BacaanServiceImpl(BacaanRepository repository,
                             ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    // ─── Bacaan CRUD ─────────────────────────────────────────────────

    @Override
    public Bacaan createBacaan(CreateBacaanRequest request, String adminUserId) {
        Bacaan bacaan = Bacaan.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .createdByUserId(adminUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return repository.saveBacaan(bacaan);
    }

    @Override
    public List<Bacaan> listBacaan(String category) {
        if (category != null && !category.isBlank()) {
            return repository.findBacaanByCategory(category);
        }
        return repository.findAllBacaan();
    }

    @Override
    public Bacaan getBacaanById(UUID id) {
        return repository.findBacaanById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));
    }

    @Override
    public void deleteBacaan(UUID id) {
        if (repository.findBacaanById(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan");
        }
        repository.deleteBacaanById(id);
    }

    // ─── Question CRUD ───────────────────────────────────────────────

    @Override
    public Question addQuestion(CreateQuestionRequest request) {
        repository.findBacaanById(request.getBacaanId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

        Question question = Question.builder()
                .id(UUID.randomUUID())
                .bacaanId(request.getBacaanId())
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctOption(request.getCorrectOption().toUpperCase())
                .createdAt(LocalDateTime.now())
                .build();

        return repository.saveQuestion(question);
    }

    @Override
    public List<Question> getQuestionsByBacaanId(UUID bacaanId) {
        return repository.findQuestionsByBacaanId(bacaanId);
    }

    @Override
    public void deleteQuestion(UUID questionId) {
        repository.deleteQuestionById(questionId);
    }

    // ─── Quiz Submission ─────────────────────────────────────────────

    @Override
    public QuizAttempt submitQuiz(UUID bacaanId, SubmitQuizRequest request) {
        // Cek apakah bacaan ada
        Bacaan bacaan = repository.findBacaanById(bacaanId)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

        // Cek apakah pelajar sudah mengerjakan kuis ini sebelumnya
        if (repository.hasUserCompletedQuiz(request.getUserId(), bacaanId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Anda sudah menyelesaikan kuis untuk bacaan ini");
        }

        // Ambil soal dan hitung skor
        List<Question> questions = repository.findQuestionsByBacaanId(bacaanId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Belum ada soal untuk bacaan ini");
        }

        Map<UUID, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        int score = 0;
        for (SubmitQuizRequest.AnswerEntry answer : request.getAnswers()) {
            Question q = questionMap.get(answer.getQuestionId());
            if (q != null && q.getCorrectOption().equalsIgnoreCase(answer.getSelectedOption())) {
                score++;
            }
        }

        // Simpan percobaan kuis
        QuizAttempt attempt = QuizAttempt.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .bacaanId(bacaanId)
                .score(score)
                .totalQuestions(questions.size())
                .completedAt(LocalDateTime.now())
                .build();

        repository.saveQuizAttempt(attempt);

        // Publish events ke modul lain (Achievements, Clan)
        Instant now = Instant.now();
        eventPublisher.publishEvent(new LearningCompletedEvent(
            request.getUserId(), bacaanId, bacaan.getTitle(), now
        ));
        eventPublisher.publishEvent(new QuizCompletedEvent(
            request.getUserId(), bacaanId, bacaan.getTitle(), score, now
        ));

        return attempt;
    }

    @Override
    public boolean hasCompletedQuiz(UUID userId, UUID bacaanId) {
        return repository.hasUserCompletedQuiz(userId, bacaanId);
    }
}
