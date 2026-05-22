package id.ac.ui.cs.advprog.yomu.learning.internal.service;

import id.ac.ui.cs.advprog.yomu.learning.internal.dto.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.*;
import id.ac.ui.cs.advprog.yomu.learning.internal.repository.BacaanRepository;
import id.ac.ui.cs.advprog.yomu.shared.event.LearningCompletedEvent;
import id.ac.ui.cs.advprog.yomu.shared.event.QuizCompletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementasi BacaanService — menangani logika bisnis untuk bacaan dan kuis.
 * Mematuhi prinsip OCP: menambah event baru tidak memerlukan perubahan di service lain.
 */
@Service
public class BacaanServiceImpl implements BacaanService {

    private final BacaanRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public BacaanServiceImpl(BacaanRepository repository,
                             RabbitTemplate rabbitTemplate,
                             MeterRegistry meterRegistry) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    // ─── Bacaan CRUD ─────────────────────────────────────────────────

    @Override
    public Bacaan createBacaan(CreateBacaanRequest request, String adminUserId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Bacaan bacaan = Bacaan.builder()
                    .id(UUID.randomUUID())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .category(request.getCategory())
                    .createdByUserId(adminUserId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Bacaan saved = repository.saveBacaan(bacaan);
            meterRegistry.counter("bacaan.created").increment();
            rabbitTemplate.convertAndSend("yomu.learning.bacaan.updated", new id.ac.ui.cs.advprog.yomu.shared.event.BacaanUpdatedEvent(
                saved.getId(), saved.getTitle(), "CREATED", Instant.now()
            ));
            return saved;
        } finally {
            sample.stop(Timer.builder("bacaan.create.time")
                .register(meterRegistry));
        }
    }

    @Override
    public List<Bacaan> listBacaan(String category) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Bacaan> result;
            if (category != null && !category.isBlank()) {
                result = repository.findBacaanByCategory(category);
                meterRegistry.counter("bacaan.list.by_category").increment();
            } else {
                result = repository.findAllBacaan();
                meterRegistry.counter("bacaan.list.all").increment();
            }
            meterRegistry.gauge("bacaan.count", result.size());
            return result;
        } finally {
            sample.stop(Timer.builder("bacaan.list.time")
                .register(meterRegistry));
        }
    }

    @Override
    public Bacaan getBacaanById(UUID id) {
        return repository.findBacaanById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));
    }

    @Override
    public Bacaan updateBacaan(UUID id, CreateBacaanRequest request) {
        Bacaan bacaan = repository.findBacaanById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

        bacaan.setTitle(request.getTitle());
        bacaan.setContent(request.getContent());
        bacaan.setCategory(request.getCategory());
        bacaan.setUpdatedAt(LocalDateTime.now());

        Bacaan saved = repository.saveBacaan(bacaan);
        rabbitTemplate.convertAndSend("yomu.learning.bacaan.updated", new id.ac.ui.cs.advprog.yomu.shared.event.BacaanUpdatedEvent(
            saved.getId(), saved.getTitle(), "UPDATED", Instant.now()
        ));
        return saved;
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
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Cek apakah bacaan ada
            Bacaan bacaan = repository.findBacaanById(bacaanId)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bacaan tidak ditemukan"));

            // Cek apakah pelajar sudah mengerjakan kuis ini sebelumnya
            if (repository.hasUserCompletedQuiz(request.getUserId(), bacaanId)) {
                meterRegistry.counter("quiz.submission.duplicate").increment();
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Anda sudah menyelesaikan kuis untuk bacaan ini");
            }

            // Ambil soal dan hitung skor
            List<Question> questions = repository.findQuestionsByBacaanId(bacaanId);
            if (questions.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Belum ada soal untuk bacaan ini");
            }

            meterRegistry.gauge("quiz.question.count", questions.size());

            Map<UUID, Question> questionMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));

            if (request.getAnswers().size() != questions.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Jumlah jawaban harus sama dengan jumlah soal");
            }

            int score = 0;
            Set<UUID> answeredQuestionIds = new HashSet<>();
            for (SubmitQuizRequest.AnswerEntry answer : request.getAnswers()) {
                if (!answeredQuestionIds.add(answer.getQuestionId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Jawaban duplikat untuk pertanyaan yang sama");
                }

                Question q = questionMap.get(answer.getQuestionId());
                if (q == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Jawaban berisi pertanyaan yang tidak termasuk bacaan ini");
                }

                if (q.getCorrectOption().equalsIgnoreCase(answer.getSelectedOption())) {
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

            // Track score metrics
            double scorePercentage = (double) score / questions.size() * 100;
            meterRegistry.counter("quiz.submission.success").increment();
            meterRegistry.gauge("quiz.score.percentage", scorePercentage);

            // Publish events ke RabbitMQ
            Instant now = Instant.now();
            rabbitTemplate.convertAndSend("yomu.learning.completed", new LearningCompletedEvent(
                request.getUserId(), bacaanId, bacaan.getTitle(), now
            ));
            rabbitTemplate.convertAndSend("yomu.quiz.completed", new QuizCompletedEvent(
                request.getUserId(), bacaanId, bacaan.getTitle(), score, questions.size(), now
            ));

            return attempt;
        } finally {
            sample.stop(Timer.builder("quiz.submission.time")
                .description("Time taken to submit and grade a quiz")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        }
    }

    @Override
    public boolean hasCompletedQuiz(UUID userId, UUID bacaanId) {
        return repository.hasUserCompletedQuiz(userId, bacaanId);
    }

    @Override
    public QuizStatsResponse getUserStats(UUID userId) {
        BacaanRepository.QuizStats stats = repository.getStatsByUserId(userId);
        List<QuizAttempt> attempts = repository.findAttemptsByUserId(userId);
        double accuracy = stats.totalAnswered() > 0
                ? (double) stats.totalCorrect() / stats.totalAnswered() * 100
                : 0.0;
        return QuizStatsResponse.builder()
                .userId(userId)
                .quizCompleted(stats.quizCompleted())
                .totalCorrect(stats.totalCorrect())
                .totalAnswered(stats.totalAnswered())
                .accuracyPercent(accuracy)
                .recentAttempts(attempts)
                .build();
    }
}
