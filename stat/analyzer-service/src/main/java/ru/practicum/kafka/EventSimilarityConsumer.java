package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-group")
    @Transactional
    public void listen(EventSimilarityAvro message) {
        Long eventA = message.getEventA();
        Long eventB = message.getEventB();
        Double score = message.getScore();
        Long timestamp = message.getTimestamp().toEpochMilli();

        log.debug("Получено сходство: eventA={}, eventB={}, score={}", eventA, eventB, score);

        Optional<EventSimilarity> existingSimilarity =
                eventSimilarityRepository.findByEventAAndEventB(eventA, eventB);

        if (existingSimilarity.isPresent()) {
            EventSimilarity similarity = existingSimilarity.get();
            similarity.setScore(score);
            similarity.setTimestamp(timestamp);
            eventSimilarityRepository.save(similarity);
            log.trace("Обновлено сходство: A={}, B={}, score={}", eventA, eventB, score);
        } else {
            EventSimilarity similarity = EventSimilarity.builder()
                    .eventA(eventA)
                    .eventB(eventB)
                    .score(score)
                    .timestamp(timestamp)
                    .build();
            eventSimilarityRepository.save(similarity);
            log.trace("Создано сходство: A={}, B={}, score={}", eventA, eventB, score);
        }
    }
}