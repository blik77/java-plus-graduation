package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    private final EventSimilarityRepository similarityRepository;
    private final UserInteractionRepository interactionRepository;

    private static final int K_NEIGHBORS = 5;

    /**
     * Метод 1: Получить сумму весов взаимодействий для списка мероприятий (популярность).
     * (Ваш код был идеален, оставляем как есть)
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
     * Метод 2: Найти мероприятия, похожие на указанное, исключая те, с которыми пользователь уже взаимодействовал.
     * (Ваша логика верна, немного причесал код для читаемости)
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
                .filter(entry -> !interactedSet.contains(entry.getKey())) // Исключаем просмотренные
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Сортировка по убыванию
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Метод: Получить рекомендации для пользователя на основе предсказания оценки (KNN).
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        List<UserInteraction> recentInteractions = interactionRepository
                .findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, maxResults));

        if (recentInteractions.isEmpty()) {
            log.info("Пользователь {} еще не взаимодействовал ни с одним мероприятием", userId);
            return Collections.emptyList();
        }

        List<Long> interactedEventIds = new ArrayList<>();
        Map<Long, Double> interactedWeights = new HashMap<>();
        for (UserInteraction interaction : recentInteractions) {
            interactedEventIds.add(interaction.getEventId());
            interactedWeights.put(interaction.getEventId(), interaction.getWeight());
        }

        List<EventSimilarity> relatedSimilarities = similarityRepository.findAllByEventAInOrEventBIn(interactedEventIds);

        Map<Long, List<Map.Entry<Long, Double>>> candidateNeighbors = new HashMap<>();

        for (EventSimilarity sim : relatedSimilarities) {
            Long candidateId = null;
            Long neighborId = null;

            if (interactedEventIds.contains(sim.getEventA()) && !interactedEventIds.contains(sim.getEventB())) {
                candidateId = sim.getEventB();
                neighborId = sim.getEventA();
            } else if (interactedEventIds.contains(sim.getEventB()) && !interactedEventIds.contains(sim.getEventA())) {
                candidateId = sim.getEventA();
                neighborId = sim.getEventB();
            }

            if (candidateId != null && neighborId != null) {
                candidateNeighbors.computeIfAbsent(candidateId, k -> new ArrayList<>())
                        .add(Map.entry(neighborId, sim.getScore()));
            }
        }

        Map<Long, Double> candidatePredictedScores = new HashMap<>();

        for (Map.Entry<Long, List<Map.Entry<Long, Double>>> entry : candidateNeighbors.entrySet()) {
            Long candidateId = entry.getKey();
            List<Map.Entry<Long, Double>> neighbors = entry.getValue();

            neighbors.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            List<Map.Entry<Long, Double>> topKNeighbors = neighbors.stream()
                    .limit(K_NEIGHBORS)
                    .collect(Collectors.toList());

            double weightedSum = 0.0;
            double similaritySum = 0.0;

            for (Map.Entry<Long, Double> neighbor : topKNeighbors) {
                Long neighborId = neighbor.getKey();
                Double similarity = neighbor.getValue();
                Double userWeight = interactedWeights.get(neighborId);

                if (userWeight != null) {
                    weightedSum += userWeight * similarity;
                    similaritySum += similarity;
                }
            }

            if (similaritySum > 0) {
                double predictedScore = weightedSum / similaritySum;
                candidatePredictedScores.put(candidateId, predictedScore);
            }
        }

        return candidatePredictedScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
}