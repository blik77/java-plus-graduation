package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SimilarityStateManager {

    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();

    private final Map<Long, Double> totalEventWeights = new ConcurrentHashMap<>();

    private final Map<String, Double> minWeights = new ConcurrentHashMap<>();

    public double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    public double getUserWeight(Long userId, Long eventId) {
        return userEventWeights.getOrDefault(userId, new ConcurrentHashMap<>()).getOrDefault(eventId, 0.0);
    }

    public void updateUserWeight(Long userId, Long eventId, double newWeight) {
        userEventWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(eventId, newWeight);
    }

    public double getTotalEventWeight(Long eventId) {
        return totalEventWeights.getOrDefault(eventId, 0.0);
    }

    public void addTotalEventWeight(Long eventId, double delta) {
        totalEventWeights.merge(eventId, delta, Double::sum);
    }

    public double getMinWeight(Long eventA, Long eventB) {
        String key = getPairKey(eventA, eventB);
        return minWeights.getOrDefault(key, 0.0);
    }

    public void addMinWeight(Long eventA, Long eventB, double delta) {
        String key = getPairKey(eventA, eventB);
        minWeights.merge(key, delta, Double::sum);
    }

    private String getPairKey(Long eventA, Long eventB) {
        return Math.min(eventA, eventB) + "_" + Math.max(eventA, eventB);
    }

    public Map<Long, Double> getUserInteractions(Long userId) {
        return userEventWeights.getOrDefault(userId, new ConcurrentHashMap<>());
    }
}