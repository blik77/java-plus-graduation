package evm.event.repository;

import evm.event.model.Event;
import evm.event.model.EventState;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> adminFilter(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Фильтр по пользователям
            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("initiatorId").in(users));
            }

            // Фильтр по статусам
            if (states != null && !states.isEmpty()) {
                List<EventState> eventStates = states.stream()
                        .map(EventState::valueOf)
                        .toList();
                predicates.add(root.get("state").in(eventStates));
            }

            // Фильтр по категориям
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            // Фильтр по дате начала
            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("eventDate"), rangeStart));
            }

            // Фильтр по дате конца
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("eventDate"), rangeEnd));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> publicFilter(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Только опубликованные события
            predicates.add(cb.equal(
                    root.get("state"), EventState.PUBLISHED));

            // Текстовый поиск по аннотации и описанию без учёта регистра
            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.toLowerCase() + "%";
                Predicate annotationMatch = cb.like(
                        cb.lower(root.get("annotation")), pattern);
                Predicate descriptionMatch = cb.like(
                        cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(annotationMatch, descriptionMatch));
            }

            // Фильтр по категориям
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            // Фильтр платные/бесплатные
            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }

            // Если rangeStart не указан — берём события позже текущего момента
            LocalDateTime start = rangeStart != null
                    ? rangeStart : LocalDateTime.now();
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));

            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
