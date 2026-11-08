package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.model.UserInteraction;
import ru.practicum.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserInteractionRepository userInteractionRepository;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-group")
    public void listen(UserActionAvro message) {
        Long userId = message.getUserId();
        Long eventId = message.getEventId();
        Long timestamp = message.getTimestamp().toEpochMilli();

        double newWeight = getWeight(message.getActionType());

        log.info("Получено действие: user={}, event={}, action={}, weight={}",
                userId, eventId, message.getActionType(), newWeight);

        Optional<UserInteraction> existingInteraction =
                userInteractionRepository.findByUserIdAndEventId(userId, eventId);

        if (existingInteraction.isPresent()) {
            UserInteraction interaction = existingInteraction.get();
            if (newWeight > interaction.getWeight()) {
                log.debug("Обновление веса: user={}, event={}, old={}, new={}",
                        userId, eventId, interaction.getWeight(), newWeight);
                interaction.setWeight(newWeight);
                interaction.setTimestamp(timestamp);
                userInteractionRepository.save(interaction);
            } else {
                log.debug("Вес не увеличился, пропускаем: user={}, event={}, current={}, new={}",
                        userId, eventId, interaction.getWeight(), newWeight);
            }
        } else {
            log.debug("Создание новой записи: user={}, event={}, weight={}", userId, eventId, newWeight);
            UserInteraction interaction = UserInteraction.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .weight(newWeight)
                    .timestamp(timestamp)
                    .build();
            userInteractionRepository.save(interaction);
        }
    }

    private double getWeight(ru.practicum.ewm.stats.avro.ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}