package ru.practicum.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SimilarityStateManager {

    private final JdbcTemplate jdbcTemplate;

    private final Map<Long, Map<Long, Double>> userEventWeights =
            new ConcurrentHashMap<>();

    private final Map<Long, Double> totalEventWeights =
            new ConcurrentHashMap<>();

    private final Map<String, Double> minWeights =
            new ConcurrentHashMap<>();

    public SimilarityStateManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_event_weights (
                    user_id BIGINT NOT NULL,
                    event_id BIGINT NOT NULL,
                    weight DOUBLE PRECISION NOT NULL,
                    PRIMARY KEY (user_id, event_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS total_event_weights (
                    event_id BIGINT PRIMARY KEY,
                    weight DOUBLE PRECISION NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS event_pair_min_weights (
                    event_a BIGINT NOT NULL,
                    event_b BIGINT NOT NULL,
                    weight DOUBLE PRECISION NOT NULL,
                    PRIMARY KEY (event_a, event_b)
                )
                """);

        jdbcTemplate.query(
                "SELECT user_id, event_id, weight FROM user_event_weights",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        userEventWeights
                                .computeIfAbsent(rs.getLong("user_id"), id -> new ConcurrentHashMap<>())
                                .put(rs.getLong("event_id"), rs.getDouble("weight"))
        );

        jdbcTemplate.query(
                "SELECT event_id, weight FROM total_event_weights",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        totalEventWeights.put(rs.getLong("event_id"), rs.getDouble("weight"))
        );

        jdbcTemplate.query(
                "SELECT event_a, event_b, weight FROM event_pair_min_weights",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        minWeights.put(
                                getPairKey(rs.getLong("event_a"), rs.getLong("event_b")),
                                rs.getDouble("weight")
                        )
        );

        log.info("Similarity state restored from persistent storage");
    }

    public double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    public double getUserWeight(Long userId, Long eventId) {
        return userEventWeights
                .getOrDefault(userId, Map.of())
                .getOrDefault(eventId, 0.0);
    }

    public void updateUserWeight(Long userId, Long eventId, double newWeight) {
        userEventWeights
                .computeIfAbsent(userId, id -> new ConcurrentHashMap<>())
                .put(eventId, newWeight);

        jdbcTemplate.update("""
            INSERT INTO user_event_weights (user_id, event_id, weight)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, event_id)
            DO UPDATE SET weight = EXCLUDED.weight
            """, userId, eventId, newWeight);
    }

    public double getTotalEventWeight(Long eventId) {
        return totalEventWeights.getOrDefault(eventId, 0.0);
    }

    public void addTotalEventWeight(Long eventId, double delta) {
        totalEventWeights.merge(eventId, delta, Double::sum);

        jdbcTemplate.update("""
            INSERT INTO total_event_weights (event_id, weight)
            VALUES (?, ?)
            ON CONFLICT (event_id)
            DO UPDATE SET weight = total_event_weights.weight + EXCLUDED.weight
            """, eventId, delta);
    }

    public double getMinWeight(Long eventA, Long eventB) {
        return minWeights.getOrDefault(getPairKey(eventA, eventB), 0.0);
    }

    public void addMinWeight(Long eventA, Long eventB, double delta) {
        String key = getPairKey(eventA, eventB);
        minWeights.merge(key, delta, Double::sum);

        long firstEventId = Math.min(eventA, eventB);
        long secondEventId = Math.max(eventA, eventB);

        jdbcTemplate.update("""
            INSERT INTO event_pair_min_weights (event_a, event_b, weight)
            VALUES (?, ?, ?)
            ON CONFLICT (event_a, event_b)
            DO UPDATE SET weight = event_pair_min_weights.weight + EXCLUDED.weight
            """, firstEventId, secondEventId, delta);
    }

    private String getPairKey(Long eventA, Long eventB) {
        return Math.min(eventA, eventB) + "_" + Math.max(eventA, eventB);
    }

    public Map<Long, Double> getUserInteractions(Long userId) {
        return userEventWeights.getOrDefault(userId, Map.of());
    }
}