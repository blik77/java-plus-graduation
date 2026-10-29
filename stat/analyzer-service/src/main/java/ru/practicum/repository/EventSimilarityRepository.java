package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    @Query("SELECT es FROM EventSimilarity es WHERE (es.eventA = :eventId OR es.eventB = :eventId) ORDER BY es.score DESC")
    List<EventSimilarity> findTopKByEventIdOrderByScoreDesc(@Param("eventId") Long eventId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA IN :eventIds OR es.eventB IN :eventIds")
    List<EventSimilarity> findAllByEventAInOrEventBIn(@Param("eventIds") List<Long> eventIds);
}
