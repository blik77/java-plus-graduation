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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityStateManager stateManager;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void listen(UserActionAvro message) {
        Long userId = message.getUserId();
        Long currentEventId = message.getEventId();

        double newWeight = stateManager.getWeight(message.getActionType());
        double oldWeight = stateManager.getUserWeight(userId, currentEventId);

        if (newWeight <= oldWeight) {
            log.debug(
                    "Вес для user={} event={} не увеличился ({} <= {}), пропускаем",
                    userId, currentEventId, newWeight, oldWeight
            );
            return;
        }

        log.info(
                "Обновление веса: user={}, event={}, old={}, new={}",
                userId, currentEventId, oldWeight, newWeight
        );

        stateManager.updateUserWeight(userId, currentEventId, newWeight);

        double deltaTotal = newWeight - oldWeight;
        stateManager.addTotalEventWeight(currentEventId, deltaTotal);

        Set<Long> affectedEvents = new HashSet<>();
        Set<Long> relatedEvents = stateManager.getRelatedEvents(currentEventId);
        if (relatedEvents != null) {
            affectedEvents.addAll(relatedEvents);
        }

        Map<Long, Double> userInteractions = stateManager.getUserInteractions(userId);
        if (userInteractions != null) {
            for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
                Long otherEventId = entry.getKey();
                if (otherEventId == null || otherEventId.equals(currentEventId)) {
                    continue;
                }

                double otherWeight = entry.getValue();
                if (otherWeight <= 0) {
                    continue;
                }

                stateManager.registerRelatedPair(currentEventId, otherEventId);
                affectedEvents.add(otherEventId);

                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double deltaMin = newMin - oldMin;

                if (deltaMin > 0) {
                    stateManager.addMinWeight(currentEventId, otherEventId, deltaMin);
                }
            }
        }

        long timestamp = message.getTimestamp().toEpochMilli();
        for (Long otherEventId : affectedEvents) {
            if (otherEventId == null || otherEventId.equals(currentEventId)) {
                continue;
            }

            double minWeight = stateManager.getMinWeight(currentEventId, otherEventId);
            double currentTotal = stateManager.getTotalEventWeight(currentEventId);
            double otherTotal = stateManager.getTotalEventWeight(otherEventId);

            if (currentTotal <= 0 || otherTotal <= 0) {
                continue;
            }

            double similarity = minWeight
                    / (Math.sqrt(currentTotal) * Math.sqrt(otherTotal));

            long eventA = Math.min(currentEventId, otherEventId);
            long eventB = Math.max(currentEventId, otherEventId);

            sendSimilarity(eventA, eventB, similarity, timestamp);
        }
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

        log.debug(
                "Отправлено сходство: A={}, B={}, score={}",
                eventA, eventB, score
        );
    }
}