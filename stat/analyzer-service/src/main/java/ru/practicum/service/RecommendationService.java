package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserInteraction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserInteractionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int K_NEIGHBORS = 5;

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    /**
     * Получить сумму весов взаимодействий для списка мероприятий.
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<Long, Double>> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Запрос сумм весов для eventIds: {}", eventIds);

        List<Object[]> results = interactionRepository.sumWeightsByEventIds(eventIds);

        log.info("Результат запроса: {} записей", results.size());
        return results.stream()
                .map(row -> Map.entry(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    /**
     * Найти похожие мероприятия, исключив те, с которыми пользователь уже взаимодействовал.
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<Long, Double>> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        List<EventSimilarity> similarities = similarityRepository.findAllByEventId(eventId);
        if (similarities.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> interactedEvents = interactionRepository.findEventIdsByUserId(userId);
        Set<Long> interactedSet = new HashSet<>(interactedEvents);

        return similarities.stream()
                .map(sim -> {
                    Long otherEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return Map.entry(otherEventId, sim.getScore());
                })
                .filter(entry -> !interactedSet.contains(entry.getKey()))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Получить рекомендации для пользователя на основе предсказания оценки (KNN).
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        List<UserInteraction> userInteractions =
                interactionRepository.findByUserIdOrderByTimestampDesc(userId);

        if (userInteractions.isEmpty()) {
            log.info("Пользователь {} еще не взаимодействовал ни с одним мероприятием", userId);
            return Collections.emptyList();
        }

        Map<Long, Double> interactedWeights = new HashMap<>();
        for (UserInteraction interaction : userInteractions) {
            interactedWeights.put(interaction.getEventId(), interaction.getWeight());
        }
        Set<Long> interactedEventIds = new HashSet<>(interactedWeights.keySet());

        List<EventSimilarity> relatedSimilarities =
                similarityRepository.findAllByEventAInOrEventBIn(new ArrayList<>(interactedEventIds));

        Map<Long, List<Map.Entry<Long, Double>>> candidateNeighbors = new HashMap<>();

        for (EventSimilarity sim : relatedSimilarities) {
            Long eventA = sim.getEventA();
            Long eventB = sim.getEventB();

            if (interactedEventIds.contains(eventA) && !interactedEventIds.contains(eventB)) {
                candidateNeighbors.computeIfAbsent(eventB, key -> new ArrayList<>())
                        .add(Map.entry(eventA, sim.getScore()));
            } else if (interactedEventIds.contains(eventB) && !interactedEventIds.contains(eventA)) {
                candidateNeighbors.computeIfAbsent(eventA, key -> new ArrayList<>())
                        .add(Map.entry(eventB, sim.getScore()));
            }
        }

        Map<Long, Double> candidatePredictedScores = new HashMap<>();

        for (Map.Entry<Long, List<Map.Entry<Long, Double>>> entry : candidateNeighbors.entrySet()) {
            List<Map.Entry<Long, Double>> neighbors = entry.getValue();
            neighbors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            double weightedSum = 0.0;
            double similaritySum = 0.0;

            for (Map.Entry<Long, Double> neighbor : neighbors.stream()
                    .limit(K_NEIGHBORS)
                    .collect(Collectors.toList())) {
                double similarity = neighbor.getValue();
                double userWeight = interactedWeights.get(neighbor.getKey());

                weightedSum += userWeight * similarity;
                similaritySum += similarity;
            }

            if (similaritySum > 0) {
                candidatePredictedScores.put(entry.getKey(), weightedSum / similaritySum);
            }
        }

        return candidatePredictedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
}