package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.service.SimilarityStateManager;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;

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
            log.debug("Вес для user={} event={} не увеличился ({} <= {}), пропускаем", userId, currentEventId, newWeight, oldWeight);
            return;
        }

        log.info("Обновление веса: user={}, event={}, old={}, new={}", userId, currentEventId, oldWeight, newWeight);

        stateManager.updateUserWeight(userId, currentEventId, newWeight);

        double deltaTotal = newWeight - oldWeight;
        stateManager.addTotalEventWeight(currentEventId, deltaTotal);

        Map<Long, Double> userInteractions = stateManager.getUserInteractions(userId);
        if (userInteractions != null) {
            for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
                Long otherEventId = entry.getKey();
                if (otherEventId.equals(currentEventId)) continue;

                double otherWeight = entry.getValue();

                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double deltaMin = newMin - oldMin;

                if (deltaMin != 0) {
                    stateManager.addMinWeight(currentEventId, otherEventId, deltaMin);
                }

                double sMin = stateManager.getMinWeight(currentEventId, otherEventId);
                double sCurrent = stateManager.getTotalEventWeight(currentEventId);
                double sOther = stateManager.getTotalEventWeight(otherEventId);

                if (sCurrent > 0 && sOther > 0) {
                    double similarity = sMin / (Math.sqrt(sCurrent) * Math.sqrt(sOther));

                    long eventA = Math.min(currentEventId, otherEventId);
                    long eventB = Math.max(currentEventId, otherEventId);

                    sendSimilarity(eventA, eventB, similarity, message.getTimestamp().toEpochMilli());
                }
            }
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
        log.debug("Отправлено сходство: A={}, B={}, score={}", eventA, eventB, score);
    }
}