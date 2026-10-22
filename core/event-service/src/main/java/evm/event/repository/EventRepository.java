package evm.event.repository;

import evm.event.model.Event;
import evm.event.model.EventState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    // События конкретного пользователя с пагинацией
    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    // Получить события по списку id — нужно для подборок (Compilation)
    List<Event> findAllByIdIn(List<Long> ids);

    // Опубликованное событие по id — для публичного API
    Optional<Event> findByIdAndState(Long id, EventState state);

    // Проверка — есть ли события в категории (перед удалением категории)
    boolean existsByCategoryId(Long categoryId);

}
