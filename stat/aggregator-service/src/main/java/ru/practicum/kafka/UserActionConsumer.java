package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.SimilarityStateManager;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private static final String TOPIC = "stats.user-actions.v1";
    private static final String OUTPUT_TOPIC = "stats.events-similarity.v1";

    private final SimilarityStateManager stateManager;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final ConsumerFactory<String, UserActionAvro> consumerFactory;
    private final KafkaListenerEndpointRegistry listenerRegistry;

    @Value("${spring.kafka.consumer.group-id:aggregator-group}")
    private String groupId;

    @PostConstruct
    public void restoreAndStartListener() {
        stateManager.clear();
        restoreHistory();

        MessageListenerContainer container = listenerRegistry.getListenerContainer("user-action-listener");
        if (container != null) {
            container.start();
        }
    }

    private void restoreHistory() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-restore-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        try (Consumer<String, UserActionAvro> consumer =
                     consumerFactory.createConsumer(groupId + "-restore-" + UUID.randomUUID(), null, null, properties)) {
            List<TopicPartition> partitions = consumer.partitionsFor(TOPIC).stream()
                    .map(partitionInfo -> new TopicPartition(TOPIC, partitionInfo.partition()))
                    .toList();

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);

            while (!historyRead(consumer, endOffsets)) {
                consumer.poll(Duration.ofMillis(500)).forEach(record ->
                        applyAction(record.value(), false)
                );
            }

            log.info("Восстановление истории Kafka завершено");
        }
    }

    private boolean historyRead(
            Consumer<String, UserActionAvro> consumer,
            Map<TopicPartition, Long> endOffsets
    ) {
        return endOffsets.entrySet().stream()
                .allMatch(entry -> consumer.position(entry.getKey()) >= entry.getValue());
    }

    @KafkaListener(
            id = "user-action-listener",
            topics = TOPIC,
            groupId = "${spring.kafka.consumer.group-id:aggregator-group}",
            autoStartup = "false"
    )
    public void listen(UserActionAvro message) {
        applyAction(message, true);
    }

    private void applyAction(UserActionAvro message, boolean publish) {
        Long userId = message.getUserId();
        Long currentEventId = message.getEventId();
        double newWeight = stateManager.getWeight(message.getActionType());
        double oldWeight = stateManager.getUserWeight(userId, currentEventId);

        if (newWeight <= oldWeight) {
            return;
        }

        Map<Long, Double> previousInteractions = stateManager.getUserInteractions(userId);
        stateManager.updateUserWeight(userId, currentEventId, newWeight);
        stateManager.addTotalEventWeight(currentEventId, newWeight - oldWeight);

        for (Map.Entry<Long, Double> entry : previousInteractions.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(currentEventId)) {
                continue;
            }

            double otherWeight = entry.getValue();
            double deltaMin = Math.min(newWeight, otherWeight) - Math.min(oldWeight, otherWeight);

            if (deltaMin != 0) {
                stateManager.addMinWeight(currentEventId, otherEventId, deltaMin);
            }

            if (publish) {
                double sumMin = stateManager.getMinWeight(currentEventId, otherEventId);
                double currentTotal = stateManager.getTotalEventWeight(currentEventId);
                double otherTotal = stateManager.getTotalEventWeight(otherEventId);

                if (currentTotal > 0 && otherTotal > 0) {
                    double similarity = sumMin
                            / (Math.sqrt(currentTotal) * Math.sqrt(otherTotal));

                    sendSimilarity(
                            Math.min(currentEventId, otherEventId),
                            Math.max(currentEventId, otherEventId),
                            similarity,
                            message.getTimestamp().toEpochMilli()
                    );
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

        kafkaTemplate.send(OUTPUT_TOPIC, eventA + "_" + eventB, similarityAvro);
        log.debug("Отправлено сходство: A={}, B={}, score={}", eventA, eventB, score);
    }
}