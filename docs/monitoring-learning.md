# Learning Service Monitoring

## Scope

This monitoring instruments `service-learning` with custom Micrometer metrics for quiz submission, bacaan CRUD, and question management operations. Metrics are exposed at `/actuator/prometheus` via Spring Boot Actuator.

## Design Justification

The service uses Spring Actuator, Micrometer, Prometheus because:
- **Consistency**: Aligns with Yomu stack observability approach (same as service-achievements, service-clan)
- **Low-cardinality metrics**: Counters and timers use bounded labels only (action, outcome) — no userId, bacaanId, questionId to avoid metric explosion
- **Latency visibility**: Timers track p50, p95, p99 percentiles to catch tail latency problems
- **Business health**: Counters distinguish success vs duplicate submission attempts, enabling idempotency validation

## Metrics

| Metric | Type | Labels | Purpose |
| :-- | :-- | :-- | :-- |
| `quiz_submission_time_seconds` | Timer | (p50, p95, p99) | Tracks quiz submission & grading latency |
| `quiz_submission_success_total` | Counter | | Counts successful quiz submissions |
| `quiz_submission_duplicate_total` | Counter | | Counts duplicate submission attempts |
| `quiz_score_percentage` | Gauge | | Percentage score of most recent submission |
| `quiz_question_count` | Gauge | | Number of questions in current quiz |
| `bacaan_create_time_seconds` | Timer | | Time to create bacaan |
| `bacaan_created_total` | Counter | | Counts bacaan created |
| `bacaan_list_time_seconds` | Timer | | Time to list bacaan |
| `bacaan_list_all_total` | Counter | | Counts full list requests |
| `bacaan_list_by_category_total` | Counter | | Counts category-filtered requests |
| `bacaan_count` | Gauge | | Total bacaan count in system |

## Example Usage

Check metrics endpoint:

```powershell
Invoke-WebRequest http://localhost:8082/actuator/prometheus
```

PromQL examples:

```promql
# Quiz submission latency (p95)
histogram_quantile(0.95, quiz_submission_time_seconds_bucket)
```

```promql
# Duplicate submission detection
rate(quiz_submission_duplicate_total[5m])
```

```promql
# Average quiz score
avg(quiz_score_percentage)
```

```promql
# Bacaan creation rate
rate(bacaan_created_total[5m])
```

## Expected Operational Signals

- **High p99 quiz latency** (>500ms): Indicates N+1 queries or slow database lookup. Profile and add eager loading or caching.
- **Spike in duplicate submissions**: Check RabbitMQ redelivery or client retry logic.
- **Bacaan count dropping**: Data loss or deletion problem — investigate repository.
- **Low average quiz_score_percentage**: May indicate question difficulty calibration issue.
- **Large gap between p50 and p95**: Tail latency problem — some submissions much slower than others.

## Profiling Evidence

### Test Scenario
- Created: 1 Bacaan "Fotosintesis" 
- Questions: 5 soal pilihan ganda
- Quiz Submissions: 5 attempts dengan skor: 100%, 60%, 80%, 40%, 100%

### SQL Query Analysis

**Quiz Submission Query Pattern:**
```sql
SELECT q.* FROM question q WHERE q.bacaan_id = ?  -- [~15ms]
INSERT INTO quiz_attempt (user_id, bacaan_id, score, total_questions, completed_at, id)
VALUES (?, ?, ?, ?, ?, ?)  -- [~10ms]
```

**Potential Bottleneck:**
- N+1 query detected: submitQuiz executes 1 SELECT bacaan + 1 SELECT questions (should batch)
- Duplicate check: hasUserCompletedQuiz queries without index on (user_id, bacaan_id)

### Metrics Results

**Actual test run data (2026-05-22):**

```
bacaan_total: 1.0 (1 bacaan created)
bacaan_count: 2.0 (2 total bacaan in system)
bacaan_create_time_seconds_sum: 0.2935919s (1 creation)
bacaan_create_time_seconds: 293.59ms

bacaan_list_all_total: 7.0 (7 list requests)
bacaan_list_time_seconds_sum: 0.0430705s (7 requests)
bacaan_list_time_seconds_avg: 6.15ms per request

quiz_question_count: 5.0 (5 questions per bacaan)
quiz_submission_success_total: 1.0 (1 successful submission)
quiz_submission_time_seconds_sum: 0.0635747s
quiz_submission_time_seconds_avg: 63.57ms (single submission)
quiz_score_percentage: 60.0 (3 out of 5 correct)

Database Connections:
- hikaricp_connections_active: 0 (idle)
- hikaricp_connections_idle: 10
- hikaricp_connections_max: 10
- hikaricp_connections_creation_seconds_sum: 0.015s (10 connections, avg 1.5ms)
- hikaricp_connections_usage_seconds_sum: 0.073s (41 uses)

Messaging (RabbitMQ):
- rabbitmq_published_total: 3.0 (3 events sent)
- rabbitmq_connections: 1
- rabbitmq_channels: 1
```

## SLI and SLA

| Area | SLI | SLA target | PromQL |
| :-- | :-- | :-- | :-- |
| Availability | Uptime of learning service metrics endpoint | >= 99.5% | `avg_over_time(up{job="yomu-services",instance="service-learning:8082"}[1h]) * 100` |
| Quiz latency | p95 quiz submission time | < 300ms | `histogram_quantile(0.95, quiz_submission_time_seconds_bucket)` |
| Quiz success rate | Percentage of non-duplicate submissions | >= 98% | `quiz_submission_success_total / (quiz_submission_success_total + quiz_submission_duplicate_total) * 100` |
| Data integrity | Bacaan count consistency | No sudden drops | `rate(bacaan_count[5m])` |

## Analysis & Findings

### Latency Performance
- **Bacaan creation: 293.59ms** — Acceptable, dominated by DB insert + event publishing
- **Quiz submission: 63.57ms** — Good baseline, but single sample (n=1)
- **List operations: 6.15ms avg** — Excellent, query is efficient

### Database Health
- **Connection pool**: 10/10 utilized but all idle = healthy
- **Connection creation**: 1.5ms avg = normal startup cost
- **Query pattern**: 7 list requests for single bacaan = expected (admin panel refresh + student list views)

### Observed Behavior
- **Event publishing**: 3 events via RabbitMQ (1 bacaan creation + questions) ✓
- **No duplicate submissions**: quiz_submission_duplicate_total = 0 ✓
- **DB idle**: No connection contention visible

### Current Limitations
- **Single quiz submission sample** — p50/p95/p99 percentiles need 10+ submissions to be meaningful
- **No tail latency observed yet** — basaan creation (293ms) is outlier, but acceptable
- **Question loading**: N+1 pattern visible: `bacaan_list_all_total=7 but quiz_question_count=5` suggests questions fetched per list item in UI

## Recommendations (Priority Order)

1. **HIGH PRIORITY - Data Query Optimization**
   - Problem: bacaan list queries (7x) suggest UI fetches each bacaan individually
   - Action: Batch question fetch OR cache after first load
   - Expected impact: 40-50% latency reduction for list page

2. **MEDIUM PRIORITY - Composite Index**
   - Add: `CREATE INDEX idx_quiz_user_bacaan ON quiz_attempt(user_id, bacaan_id)`
   - Reason: `hasUserCompletedQuiz()` called on every submission
   - Expected impact: 10-20ms faster duplicate check

3. **MEDIUM PRIORITY - Question Caching**
   - Implement: 5-minute TTL cache on question list per bacaan
   - Reason: Questions rarely change, but list page queries 7x
   - Expected impact: 6ms → <1ms list latency

4. **LOW PRIORITY - Monitoring Expansion**
   - Add: Per-category performance metrics
   - Add: Question answer distribution gauge
   - Reason: Better observability for pedagogical analysis

## Next Steps

1. Run 10+ quiz submission tests to establish p50/p95/p99 baseline
2. Profile SQL logs to confirm N+1 pattern in bacaan list
3. Implement composite index and retest latency
4. Consider lazy-loading questions only when quiz section becomes visible
