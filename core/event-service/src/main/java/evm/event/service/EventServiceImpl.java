package evm.event.service;

import evm.category.model.Category;
import evm.category.repository.CategoryRepository;
import evm.event.client.RequestClient;
import evm.event.client.UserClient;
import evm.event.dto.*;
import evm.event.mapper.EventMapper;
import evm.event.mapper.LocationMapper;
import evm.event.model.Event;
import evm.event.model.EventState;
import evm.event.repository.EventRepository;
import evm.event.repository.EventSpecification;
import evm.common.exceptions.ConflictException;
import evm.common.exceptions.NotFoundException;
import evm.stat.client.StatsClient;
import evm.stat.dto.HitDto;
import evm.stat.dto.ViewStatsDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String APP_NAME = "ewm-main-service";
    private static final int MIN_HOURS_BEFORE_EVENT_USER = 2;
    private static final int MIN_HOURS_BEFORE_EVENT_ADMIN = 1;

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatsClient statsClient;

// =========================================================================
// PUBLIC
// =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findAllById(ids);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
        Map<Long, String> userNamesMap = userClient.getUsersByIds(initiatorIds).stream()
            .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return events.stream()
            .map(e -> EventMapper.toShortDto(
                e,
                viewsMap.getOrDefault(e.getId(), 0L),
                confirmedMap.getOrDefault(e.getId(), 0L),
                userNamesMap.getOrDefault(e.getInitiatorId(), "Unknown")
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size,
                                               HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart не может быть позже rangeEnd");
        }

        sendHit(request);

        Pageable pageable = "EVENT_DATE".equals(sort)
            ? PageRequest.of(from / size, size, Sort.by("eventDate").ascending())
            : PageRequest.of(from / size, size);

        Specification<Event> spec = EventSpecification.publicFilter(text, categories, paid, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        if (events.isEmpty()) return List.of();

        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                .filter(e -> e.getParticipantLimit() == 0 ||
                    e.getParticipantLimit() > confirmedMap.getOrDefault(e.getId(), 0L))
                .collect(Collectors.toList());
        }

        Map<Long, Long> viewsMap = getViewsMap(events);

        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());

        Map<Long, String> userNamesMap = userClient.getUsersByIds(initiatorIds).stream()
            .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        List<EventShortDto> result = events.stream()
            .map(e -> EventMapper.toShortDto(
                e,
                viewsMap.getOrDefault(e.getId(), 0L),
                confirmedMap.getOrDefault(e.getId(), 0L),
                userNamesMap.getOrDefault(e.getInitiatorId(), "Unknown") // 👈 БЕРЕМ ИМЯ ИЗ МАПЫ!
            ))
            .collect(Collectors.toList());

        if ("VIEWS".equals(sort)) {
            result.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
            .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        sendHit(request);
        Long views = getViews(event);
        Long confirmed = 0L;

        String userName = userClient.getUserById(event.getInitiatorId()).getName();
        return EventMapper.toFullDto(event, views, confirmed, userName);
    }

// =========================================================================
// PRIVATE
// =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        String userName = userClient.getUserById(userId).getName();

        return events.stream()
            .map(e -> EventMapper.toShortDto(
                e,
                viewsMap.getOrDefault(e.getId(), 0L),
                confirmedMap.getOrDefault(e.getId(), 0L),
                userName
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        validateEventDate(dto.getEventDate(), MIN_HOURS_BEFORE_EVENT_USER);

        Category category = getCategoryOrThrow(dto.getCategory());
        Event event = EventMapper.toEntity(dto, category, userId);
        Event saved = eventRepository.save(event);

        String userName = userClient.getUserById(userId).getName();
        return EventMapper.toFullDto(saved, 0L, 0L, userName);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        Long views = getViews(event);
        Long confirmed = 0L;

        String userName = userClient.getUserById(event.getInitiatorId()).getName();
        return EventMapper.toFullDto(event, views, confirmed, userName);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Изменять можно только события в статусе PENDING или CANCELED");
        }

        applyUserUpdates(event, dto);
        Event saved = eventRepository.save(event);
        Long confirmed = 0L;

        String userName = userClient.getUserById(event.getInitiatorId()).getName();
        return EventMapper.toFullDto(saved, getViews(saved), confirmed, userName);
    }

// =========================================================================
// ADMIN
// =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<String> states,
                                             List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        Specification<Event> spec = EventSpecification.adminFilter(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        if (events.isEmpty()) return List.of();

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
        Map<Long, String> userNamesMap = userClient.getUsersByIds(initiatorIds).stream()
            .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return events.stream()
            .map(e -> EventMapper.toFullDto(
                e,
                viewsMap.getOrDefault(e.getId(), 0L),
                confirmedMap.getOrDefault(e.getId(), 0L),
                userNamesMap.getOrDefault(e.getInitiatorId(), "Unknown")
            ))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventOrThrow(eventId);
        applyAdminUpdates(event, dto);
        Event saved = eventRepository.save(event);
        Long confirmed = 0L;

        String userName = userClient.getUserById(event.getInitiatorId()).getName();
        return EventMapper.toFullDto(saved, getViews(saved), confirmed, userName);
    }

// =========================================================================
// Вспомогательные методы
// =========================================================================

    // Отправляет хит в сервис статистики
    private void sendHit(HttpServletRequest request) {
        statsClient.hit(HitDto.builder()
            .app(APP_NAME)
            .uri(request.getRequestURI())
            .ip(request.getRemoteAddr())
            .timestamp(LocalDateTime.now())
            .build());
    }

    // Получает количество просмотров для одного события из сервиса статистики
    private Long getViews(Event event) {
        List<ViewStatsDto> stats = statsClient.getStats(
            event.getCreatedOn(),
            LocalDateTime.now(),
            List.of("/events/" + event.getId()),
            true
        );
        return stats.isEmpty() ? 0L : stats.get(0).getHits();
    }

    // Получает просмотры для списка событий одним запросом к сервису статистики
    // Возвращает Map: eventId → количество просмотров
    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        try {
            List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());
            LocalDateTime earliest = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(5));
            List<ViewStatsDto> stats = statsClient.getStats(
                earliest, LocalDateTime.now(), uris, true);
            return stats.stream().collect(Collectors.toMap(
                s -> Long.parseLong(s.getUri().replace("/events/", "")),
                ViewStatsDto::getHits
            ));
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            return Map.of();
        }
    }

    // Получает количество подтверждённых заявок для списка событий
    // Возвращает Map: eventId → количество заявок
    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        try {
            List<Long> eventIds = events.stream()
                .map(Event::getId)
                .distinct()
                .collect(Collectors.toList());

            return requestClient.getConfirmedCounts(eventIds);

        } catch (Exception e) {
            log.warn("Не удалось получить количество подтвержденных заявок: {}", e.getMessage());
            return Map.of();
        }
    }

    // Применяет изменения из запроса пользователя к событию
    // null-поля пропускаются — означают "не менять"
    private void applyUserUpdates(Event event, UpdateEventUserRequest dto) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getLocation() != null) event.setLocation(LocationMapper.toEntity(dto.getLocation()));

        if (dto.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(dto.getCategory()));
        }

        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate(), MIN_HOURS_BEFORE_EVENT_USER);
            event.setEventDate(dto.getEventDate());
        }

        // Изменение статуса через stateAction
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new IllegalArgumentException(
                        "Неизвестное действие для изменения статуса (stateAction): " + dto.getStateAction());
            }
        }
    }

    // Применяет изменения из запроса администратора к событию
    private void applyAdminUpdates(Event event, UpdateEventAdminRequest dto) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getLocation() != null) event.setLocation(LocationMapper.toEntity(dto.getLocation()));

        if (dto.getCategory() != null) {
            event.setCategory(getCategoryOrThrow(dto.getCategory()));
        }

        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate(), MIN_HOURS_BEFORE_EVENT_ADMIN);
            event.setEventDate(dto.getEventDate());
        }

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "PUBLISH_EVENT" -> {
                    // Публиковать можно только PENDING событие
                    if (!EventState.PENDING.equals(event.getState())) {
                        throw new ConflictException(
                                "Невозможно опубликовать событие, так как оно находится не в подходящем статусе: "
                                        + event.getState());
                    }
                    // Дата события не раньше чем через час после публикации
                    validateEventDate(event.getEventDate(), MIN_HOURS_BEFORE_EVENT_ADMIN);
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case "REJECT_EVENT" -> {
                    // Отклонять можно только не опубликованное событие
                    if (EventState.PUBLISHED.equals(event.getState())) {
                        throw new ConflictException(
                                "Невозможно отклонить событие, так как оно уже опубликовано");
                    }
                    event.setState(EventState.CANCELED);
                }
                default -> throw new IllegalArgumentException(
                        "Неизвестное действие для изменения статуса (stateAction): " + dto.getStateAction());
            }
        }
    }

    private void validateEventDate(LocalDateTime eventDate, int minHours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(minHours))) {
            throw new IllegalArgumentException(
                    "Поле: eventDate. Error: должно содержать дату не ранее чем через "
                            + minHours + " час(а) от текущего момента. Value: " + eventDate);
        }
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }

    @Override
    @Transactional(readOnly = true)
    public EventInternalInfoDto getEventByIdInternal(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        String initiatorName = "Unknown";
        if (event.getInitiatorId() != null) {
            try {
                initiatorName = userClient.getUserById(event.getInitiatorId()).getName();
            } catch (Exception e) {
                log.warn("Не удалось получить имя пользователя с id={}", event.getInitiatorId());
            }
        }

        return EventMapper.toInternalInfoDto(event, initiatorName);

    }
}