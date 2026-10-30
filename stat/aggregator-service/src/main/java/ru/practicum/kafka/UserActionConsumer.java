package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.SimilarityStateManager;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityStateManager stateManager;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    /**
     * Известные пары, чьи коэффициенты могут измениться при обновлении веса события.
     * Ключ — событие, значение — связанные с ним события.
     */
    private final Map<Long, Set<Long>> relatedEvents = new ConcurrentHashMap<>();

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(UserActionAvro message) {
        Long userId = message.getUserId();
        Long currentEventId = message.getEventId();
        double newWeight = stateManager.getWeight(message.getActionType());
        double oldWeight = stateManager.getUserWeight(userId, currentEventId);

        if (newWeight <= oldWeight) {
            log.debug("Вес для user={} event={} не увеличился ({} <= {}), пропускаем",
                    userId, currentEventId, newWeight, oldWeight);
            return;
        }

        log.info("Обновление веса: user={}, event={}, old={}, new={}",
                userId, currentEventId, oldWeight, newWeight);

        stateManager.updateUserWeight(userId, currentEventId, newWeight);

        double deltaTotal = newWeight - oldWeight;
        stateManager.addTotalEventWeight(currentEventId, deltaTotal);

        Map<Long, Double> userInteractions = stateManager.getUserInteractions(userId);
        if (userInteractions != null) {
            for (Long otherEventId : userInteractions.keySet()) {
                if (!otherEventId.equals(currentEventId)) {
                    registerRelatedEvents(currentEventId, otherEventId);
                }
            }
        }

        // Обновление суммарного веса currentEventId влияет на знаменатель
        // коэффициента для каждой известной пары с этим событием.
        Set<Long> related = relatedEvents.get(currentEventId);
        if (related != null) {
            for (Long otherEventId : related) {
                recalculateAndSendSimilarity(
                        currentEventId,
                        otherEventId,
                        message.getTimestamp().toEpochMilli()
                );
            }
        }
    }

    private void registerRelatedEvents(Long eventA, Long eventB) {
        relatedEvents.computeIfAbsent(eventA, key -> ConcurrentHashMap.newKeySet()).add(eventB);
        relatedEvents.computeIfAbsent(eventB, key -> ConcurrentHashMap.newKeySet()).add(eventA);
    }

    private void recalculateAndSendSimilarity(long eventId, long otherEventId, long timestamp) {
        double minWeight = stateManager.getMinWeight(eventId, otherEventId);
        double eventWeight = stateManager.getTotalEventWeight(eventId);
        double otherEventWeight = stateManager.getTotalEventWeight(otherEventId);

        if (eventWeight <= 0 || otherEventWeight <= 0) {
            return;
        }

        double similarity = minWeight
                / (Math.sqrt(eventWeight) * Math.sqrt(otherEventWeight));

        long eventA = Math.min(eventId, otherEventId);
        long eventB = Math.max(eventId, otherEventId);

        sendSimilarity(eventA, eventB, similarity, timestamp);
    }

    private void sendSimilarity(long eventA, long eventB, double score, long timestamp) {
        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .build();

        String key = eventA + "_" + eventB;

        kafkaTemplate.send("stats.events-similarity.v1", key, similarityAvro);
        log.debug("Отправлено сходство: A={}, B={}, score={}", eventA, eventB, score);
    }
}