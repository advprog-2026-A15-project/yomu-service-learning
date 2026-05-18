package id.ac.ui.cs.advprog.yomu.learning.repository;

import id.ac.ui.cs.advprog.yomu.learning.internal.model.Bacaan;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.Question;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.QuizAttempt;
import id.ac.ui.cs.advprog.yomu.learning.internal.repository.BacaanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BacaanRepositoryTest {

    BacaanRepository repository;

    UUID bacaanId;
    UUID userId;

    @BeforeEach
    void setUp() {
        String dbName = "testdb_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");

        repository = new BacaanRepository(new JdbcTemplate(ds));
        repository.createTables();

        bacaanId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    Bacaan buildBacaan() {
        return Bacaan.builder()
                .id(bacaanId)
                .title("Judul Test")
                .content("Isi test")
                .category("News")
                .createdByUserId("admin-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    Question buildQuestion(UUID bId) {
        return Question.builder()
                .id(UUID.randomUUID())
                .bacaanId(bId)
                .questionText("Pertanyaan?")
                .optionA("A").optionB("B").optionC("C").optionD("D")
                .correctOption("A")
                .createdAt(LocalDateTime.now())
                .build();
    }

    QuizAttempt buildAttempt(UUID uId, UUID bId) {
        return QuizAttempt.builder()
                .id(UUID.randomUUID())
                .userId(uId)
                .bacaanId(bId)
                .score(3)
                .totalQuestions(5)
                .completedAt(LocalDateTime.now())
                .build();
    }

    // ─── Bacaan ──────────────────────────────────────────────────────

    @Test
    void saveBacaan_andFindById() {
        repository.saveBacaan(buildBacaan());

        Optional<Bacaan> found = repository.findBacaanById(bacaanId);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Judul Test");
        assertThat(found.get().getCategory()).isEqualTo("News");
    }

    @Test
    void findAllBacaan_returnsAll() {
        repository.saveBacaan(buildBacaan());

        List<Bacaan> all = repository.findAllBacaan();

        assertThat(all).isNotEmpty();
    }

    @Test
    void findBacaanByCategory_filtersCorrectly() {
        repository.saveBacaan(buildBacaan());

        List<Bacaan> news = repository.findBacaanByCategory("News");
        List<Bacaan> sport = repository.findBacaanByCategory("Sport");

        assertThat(news).isNotEmpty();
        assertThat(sport).isEmpty();
    }

    @Test
    void updateBacaan_viaResave() {
        Bacaan bacaan = buildBacaan();
        repository.saveBacaan(bacaan);

        bacaan.setTitle("Updated Title");
        bacaan.setUpdatedAt(LocalDateTime.now());
        repository.saveBacaan(bacaan);

        Optional<Bacaan> updated = repository.findBacaanById(bacaanId);
        assertThat(updated.get().getTitle()).isEqualTo("Updated Title");
    }

    @Test
    void deleteBacaanById_removesRecord() {
        repository.saveBacaan(buildBacaan());

        int deleted = repository.deleteBacaanById(bacaanId);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findBacaanById(bacaanId)).isEmpty();
    }

    @Test
    void findBacaanById_notFound_returnsEmpty() {
        assertThat(repository.findBacaanById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void deleteBacaan_alsoCascadesQuestionsAndAttempts() {
        repository.saveBacaan(buildBacaan());
        repository.saveQuestion(buildQuestion(bacaanId));
        repository.saveQuizAttempt(buildAttempt(userId, bacaanId));

        repository.deleteBacaanById(bacaanId);

        assertThat(repository.findQuestionsByBacaanId(bacaanId)).isEmpty();
        assertThat(repository.hasUserCompletedQuiz(userId, bacaanId)).isFalse();
    }

    // ─── Question ────────────────────────────────────────────────────

    @Test
    void saveQuestion_andFindByBacaanId() {
        repository.saveBacaan(buildBacaan());
        Question q = buildQuestion(bacaanId);
        repository.saveQuestion(q);

        List<Question> result = repository.findQuestionsByBacaanId(bacaanId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCorrectOption()).isEqualTo("A");
    }

    @Test
    void deleteQuestionById_removesQuestion() {
        repository.saveBacaan(buildBacaan());
        Question q = buildQuestion(bacaanId);
        repository.saveQuestion(q);

        int deleted = repository.deleteQuestionById(q.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findQuestionsByBacaanId(bacaanId)).isEmpty();
    }

    @Test
    void findQuestionsByBacaanId_empty_returnsEmptyList() {
        repository.saveBacaan(buildBacaan());
        assertThat(repository.findQuestionsByBacaanId(bacaanId)).isEmpty();
    }

    // ─── QuizAttempt ─────────────────────────────────────────────────

    @Test
    void saveQuizAttempt_andCheckCompleted() {
        repository.saveBacaan(buildBacaan());
        repository.saveQuizAttempt(buildAttempt(userId, bacaanId));

        assertThat(repository.hasUserCompletedQuiz(userId, bacaanId)).isTrue();
        assertThat(repository.hasUserCompletedQuiz(UUID.randomUUID(), bacaanId)).isFalse();
    }

    @Test
    void findAttemptsByUserId_returnsUserAttempts() {
        repository.saveBacaan(buildBacaan());
        repository.saveQuizAttempt(buildAttempt(userId, bacaanId));

        List<QuizAttempt> attempts = repository.findAttemptsByUserId(userId);

        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getScore()).isEqualTo(3);
        assertThat(attempts.get(0).getTotalQuestions()).isEqualTo(5);
    }

    @Test
    void getStatsByUserId_aggregatesCorrectly() {
        UUID bacaanId2 = UUID.randomUUID();
        Bacaan b2 = Bacaan.builder()
                .id(bacaanId2).title("B2").content("c").category("Sport")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        repository.saveBacaan(buildBacaan());
        repository.saveBacaan(b2);
        repository.saveQuizAttempt(buildAttempt(userId, bacaanId));
        repository.saveQuizAttempt(buildAttempt(userId, bacaanId2));

        BacaanRepository.QuizStats stats = repository.getStatsByUserId(userId);

        assertThat(stats.quizCompleted()).isEqualTo(2);
        assertThat(stats.totalCorrect()).isEqualTo(6);
        assertThat(stats.totalAnswered()).isEqualTo(10);
    }

    @Test
    void getStatsByUserId_noAttempts_returnsZeros() {
        BacaanRepository.QuizStats stats = repository.getStatsByUserId(UUID.randomUUID());

        assertThat(stats.quizCompleted()).isZero();
        assertThat(stats.totalCorrect()).isZero();
        assertThat(stats.totalAnswered()).isZero();
    }
}
