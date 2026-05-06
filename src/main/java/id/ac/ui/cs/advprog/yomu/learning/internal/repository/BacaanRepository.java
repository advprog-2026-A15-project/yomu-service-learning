package id.ac.ui.cs.advprog.yomu.learning.internal.repository;

import id.ac.ui.cs.advprog.yomu.learning.internal.model.Bacaan;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.Question;
import id.ac.ui.cs.advprog.yomu.learning.internal.model.QuizAttempt;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JDBC untuk mengakses data Bacaan, Question, dan QuizAttempt.
 * Menggunakan JdbcTemplate secara langsung tanpa ORM untuk mematuhi
 * pola konsisten dengan layanan lain dalam monorepo ini.
 */
@Repository
public class BacaanRepository {

    private final JdbcTemplate jdbcTemplate;

    public BacaanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void createTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS bacaan (
                id UUID PRIMARY KEY,
                title VARCHAR(500) NOT NULL,
                content TEXT NOT NULL,
                category VARCHAR(100) NOT NULL,
                created_by_user_id VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS questions (
                id UUID PRIMARY KEY,
                bacaan_id UUID NOT NULL,
                question_text TEXT NOT NULL,
                option_a VARCHAR(500) NOT NULL,
                option_b VARCHAR(500) NOT NULL,
                option_c VARCHAR(500) NOT NULL,
                option_d VARCHAR(500) NOT NULL,
                correct_option VARCHAR(1) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS quiz_attempts (
                id UUID PRIMARY KEY,
                user_id UUID NOT NULL,
                bacaan_id UUID NOT NULL,
                score INTEGER NOT NULL,
                total_questions INTEGER NOT NULL,
                completed_at TIMESTAMP NOT NULL
            )
        """);
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_quiz_attempts_user_bacaan
            ON quiz_attempts (user_id, bacaan_id)
        """);
    }

    // ─── Bacaan ──────────────────────────────────────────────────────

    private final RowMapper<Bacaan> bacaanRowMapper = (rs, rowNum) -> Bacaan.builder()
            .id(rs.getObject("id", UUID.class))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .category(rs.getString("category"))
            .createdByUserId(rs.getString("created_by_user_id"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();

    public Bacaan saveBacaan(Bacaan bacaan) {
        if (bacaan.getId() == null) bacaan.setId(UUID.randomUUID());
        if (bacaan.getCreatedAt() == null) bacaan.setCreatedAt(LocalDateTime.now());
        if (bacaan.getUpdatedAt() == null) bacaan.setUpdatedAt(LocalDateTime.now());

        jdbcTemplate.update("""
            INSERT INTO bacaan (id, title, content, category, created_by_user_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            bacaan.getId(), bacaan.getTitle(), bacaan.getContent(),
            bacaan.getCategory(), bacaan.getCreatedByUserId(),
            Timestamp.valueOf(bacaan.getCreatedAt()),
            Timestamp.valueOf(bacaan.getUpdatedAt())
        );
        return bacaan;
    }

    public List<Bacaan> findAllBacaan() {
        return jdbcTemplate.query(
            "SELECT * FROM bacaan ORDER BY created_at DESC",
            bacaanRowMapper
        );
    }

    public List<Bacaan> findBacaanByCategory(String category) {
        return jdbcTemplate.query(
            "SELECT * FROM bacaan WHERE category = ? ORDER BY created_at DESC",
            bacaanRowMapper, category
        );
    }

    public Optional<Bacaan> findBacaanById(UUID id) {
        return jdbcTemplate.query(
            "SELECT * FROM bacaan WHERE id = ?",
            bacaanRowMapper, id
        ).stream().findFirst();
    }

    public int deleteBacaanById(UUID id) {
        jdbcTemplate.update("DELETE FROM questions WHERE bacaan_id = ?", id);
        jdbcTemplate.update("DELETE FROM quiz_attempts WHERE bacaan_id = ?", id);
        return jdbcTemplate.update("DELETE FROM bacaan WHERE id = ?", id);
    }

    // ─── Questions ───────────────────────────────────────────────────

    private final RowMapper<Question> questionRowMapper = (rs, rowNum) -> Question.builder()
            .id(rs.getObject("id", UUID.class))
            .bacaanId(rs.getObject("bacaan_id", UUID.class))
            .questionText(rs.getString("question_text"))
            .optionA(rs.getString("option_a"))
            .optionB(rs.getString("option_b"))
            .optionC(rs.getString("option_c"))
            .optionD(rs.getString("option_d"))
            .correctOption(rs.getString("correct_option"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public Question saveQuestion(Question q) {
        if (q.getId() == null) q.setId(UUID.randomUUID());
        if (q.getCreatedAt() == null) q.setCreatedAt(LocalDateTime.now());

        jdbcTemplate.update("""
            INSERT INTO questions (id, bacaan_id, question_text, option_a, option_b, option_c, option_d, correct_option, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            q.getId(), q.getBacaanId(), q.getQuestionText(),
            q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
            q.getCorrectOption(), Timestamp.valueOf(q.getCreatedAt())
        );
        return q;
    }

    public List<Question> findQuestionsByBacaanId(UUID bacaanId) {
        return jdbcTemplate.query(
            "SELECT * FROM questions WHERE bacaan_id = ? ORDER BY created_at ASC",
            questionRowMapper, bacaanId
        );
    }

    public int deleteQuestionById(UUID id) {
        return jdbcTemplate.update("DELETE FROM questions WHERE id = ?", id);
    }

    // ─── Quiz Attempts ───────────────────────────────────────────────

    private final RowMapper<QuizAttempt> attemptRowMapper = (rs, rowNum) -> QuizAttempt.builder()
            .id(rs.getObject("id", UUID.class))
            .userId(rs.getObject("user_id", UUID.class))
            .bacaanId(rs.getObject("bacaan_id", UUID.class))
            .score(rs.getInt("score"))
            .totalQuestions(rs.getInt("total_questions"))
            .completedAt(rs.getTimestamp("completed_at").toLocalDateTime())
            .build();

    public QuizAttempt saveQuizAttempt(QuizAttempt attempt) {
        if (attempt.getId() == null) attempt.setId(UUID.randomUUID());
        if (attempt.getCompletedAt() == null) attempt.setCompletedAt(LocalDateTime.now());

        jdbcTemplate.update("""
            INSERT INTO quiz_attempts (id, user_id, bacaan_id, score, total_questions, completed_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            attempt.getId(), attempt.getUserId(), attempt.getBacaanId(),
            attempt.getScore(), attempt.getTotalQuestions(),
            Timestamp.valueOf(attempt.getCompletedAt())
        );
        return attempt;
    }

    public boolean hasUserCompletedQuiz(UUID userId, UUID bacaanId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM quiz_attempts WHERE user_id = ? AND bacaan_id = ?",
            Integer.class, userId, bacaanId
        );
        return count != null && count > 0;
    }

    public List<QuizAttempt> findAttemptsByUserId(UUID userId) {
        return jdbcTemplate.query(
            "SELECT * FROM quiz_attempts WHERE user_id = ? ORDER BY completed_at DESC",
            attemptRowMapper, userId
        );
    }
}
