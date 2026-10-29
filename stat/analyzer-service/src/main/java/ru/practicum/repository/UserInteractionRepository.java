package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserInteraction;

import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.userId = :userId ORDER BY ui.timestamp DESC")
    List<UserInteraction> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT ui.eventId FROM UserInteraction ui WHERE ui.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ui.eventId, SUM(ui.weight) FROM UserInteraction ui WHERE ui.eventId IN :eventIds GROUP BY ui.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}