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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

        double deltaTotal = newWeight - oldWeight;
        double updatedCurrentTotal =
                stateManager.getTotalEventWeight(currentEventId) + deltaTotal;

        Map<Long, Double> userInteractions = stateManager.getUserInteractions(userId);
        Map<Long, Double> minWeightDeltas = new HashMap<>();
        List<CompletableFuture<?>> sendFutures = new ArrayList<>();

        if (userInteractions != null) {
            for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
                Long otherEventId = entry.getKey();
                if (otherEventId.equals(currentEventId)) {
                    continue;
                }

                double otherWeight = entry.getValue();

                double oldMin = Math.min(oldWeight, otherWeight);
                double newMin = Math.min(newWeight, otherWeight);
                double deltaMin = newMin - oldMin;

                if (deltaMin != 0) {
                    minWeightDeltas.put(otherEventId, deltaMin);
                }

                double updatedMin = stateManager.getMinWeight(currentEventId, otherEventId) + deltaMin;
                double otherTotal = stateManager.getTotalEventWeight(otherEventId);

                if (updatedCurrentTotal > 0 && otherTotal > 0) {
                    double similarity =
                            updatedMin / (Math.sqrt(updatedCurrentTotal) * Math.sqrt(otherTotal));

                    long eventA = Math.min(currentEventId, otherEventId);
                    long eventB = Math.max(currentEventId, otherEventId);

                    sendFutures.add(sendSimilarity(
                            eventA,
                            eventB,
                            similarity,
                            message.getTimestamp().toEpochMilli()
                    ));
                }
            }
        }

        CompletableFuture.allOf(sendFutures.toArray(new CompletableFuture<?>[0])).join();

        log.info(
                "Обновление веса: user={}, event={}, old={}, new={}",
                userId, currentEventId, oldWeight, newWeight
        );

        stateManager.updateUserWeight(userId, currentEventId, newWeight);
        stateManager.addTotalEventWeight(currentEventId, deltaTotal);

        minWeightDeltas.forEach((otherEventId, deltaMin) ->
                stateManager.addMinWeight(currentEventId, otherEventId, deltaMin)
        );
    }

    private CompletableFuture<?> sendSimilarity(
            long eventA,
            long eventB,
            double score,
            long timestamp
    ) {
        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .build();

        String key = eventA + "_" + eventB;

        log.debug("Отправка сходства: A={}, B={}, score={}", eventA, eventB, score);
        return kafkaTemplate.send("stats.events-similarity.v1", key, similarityAvro);
    }
}