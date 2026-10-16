package evm.main.event.service;

import evm.main.category.model.Category;
import evm.main.category.repository.CategoryRepository;
import evm.main.event.dto.*;
import evm.main.event.mapper.EventMapper;
import evm.main.event.mapper.LocationMapper;
import evm.main.event.model.Event;
import evm.main.event.model.EventState;
import evm.main.event.repository.EventRepository;
import evm.main.event.repository.EventSpecification;
import evm.main.exceptions.ConflictException;
import evm.main.exceptions.NotFoundException;
import evm.main.requests.dto.EventRequestStatusUpdateRequest;
import evm.main.requests.dto.EventRequestStatusUpdateResult;
import evm.main.requests.dto.ParticipationRequestDto;
import evm.main.requests.mapper.RequestMapper;
import evm.main.requests.model.Request;
import evm.main.requests.model.Status;
import evm.main.requests.repository.RequestRepositoryJpa;
import evm.main.users.model.User;
import evm.main.users.repository.UserRepositoryJpa;
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
    private final UserRepositoryJpa userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepositoryJpa requestRepository;
    private final RequestMapper mapper;
    private final StatsClient statsClient;

// =========================================================================
// PUBLIC
// =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size,
                                               HttpServletRequest request) {
        // Валидация диапазона дат
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart не может быть позже rangeEnd");
        }

        sendHit(request);

        // Сортировка
        Pageable pageable;
        if ("EVENT_DATE".equals(sort)) {
            pageable = PageRequest.of(from / size, size,
                    Sort.by("eventDate").ascending());
        } else {
            pageable = PageRequest.of(from / size, size);
        }

        Specification<Event> spec = EventSpecification.publicFilter(
                text, categories, paid, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        // Фильтрация по доступности
        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            e.getParticipantLimit() > confirmedMap.getOrDefault(e.getId(), 0L))
                    .collect(Collectors.toList());
        }

        // Получаем статистику и количество заявок для всех событий разом
        Map<Long, Long> viewsMap = getViewsMap(events);

        List<EventShortDto> result = events.stream()
                .map(e -> EventMapper.toShortDto(
                        e,
                        viewsMap.getOrDefault(e.getId(), 0L),
                        confirmedMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());

        // Сортировка по просмотрам если запрошена
        if ("VIEWS".equals(sort)) {
            result.sort(Comparator.comparingLong(EventShortDto::getViews).reversed());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEventById(Long id, HttpServletRequest request) {
        // Только опубликованные события доступны публично
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));

        // Фиксируем обращение в сервисе статистики
        sendHit(request);

        Long views = getViews(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(id, Status.CONFIRMED);

        return EventMapper.toFullDto(event, views, confirmed);
    }

// =========================================================================
// PRIVATE
// =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        checkUserExists(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        return events.stream()
                .map(e -> EventMapper.toShortDto(
                        e,
                        viewsMap.getOrDefault(e.getId(), 0L),
                        confirmedMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        // Проверяем что дата события не раньше чем через 2 часа
        validateEventDate(dto.getEventDate(), MIN_HOURS_BEFORE_EVENT_USER);

        User user = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());

        Event event = EventMapper.toEntity(dto, category, user);
        Event saved = eventRepository.save(event);

        return EventMapper.toFullDto(saved, 0L, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        // Пользователь может видеть только своё событие
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        Long views = getViews(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);

        return EventMapper.toFullDto(event, views, confirmed);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId,
                                        UpdateEventUserRequest dto) {
        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        // Пользователь может редактировать только своё событие
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        // Нельзя редактировать опубликованное событие
        if (EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Изменять можно только события в статусе PENDING или CANCELED");
        }

        // Применяем изменения — только те поля которые не null
        applyUserUpdates(event, dto);

        Event saved = eventRepository.save(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);

        return EventMapper.toFullDto(saved, getViews(saved), confirmed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        checkUserExists(userId);
        Event event = getEventOrThrow(eventId);

        // Проверка прав организатора
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        List<Long> requestedIds = dto.getRequestIds();

        // Получаем заявки только для этого события
        List<Request> requests = requestRepository.findAllByIdInAndEventId(dto.getRequestIds(), eventId);

        if (requests.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Обнаружены ID заявок, не принадлежащих данному событию. " +
                    "Проверьте корректность переданных requestIds.");
        }

        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены для события " + eventId);
        }

        // Если модерация отключена или лимит = 0 — подтверждение не требуется
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            // Автоматически подтверждаем все заявки
            requests.forEach(r -> r.setStatus(Status.CONFIRMED));
            requestRepository.saveAll(requests);

            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(requests.stream()
                            .map(mapper::toDto)
                            .collect(Collectors.toList()))
                    .rejectedRequests(List.of())
                    .build();
        }

        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);

        // Лимит уже достигнут — нельзя подтверждать новые заявки
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников достигнут");
        }

        // Проверяем что все заявки в статусе PENDING
        requests.forEach(r -> {
            if (!Status.PENDING.equals(r.getStatus())) {
                throw new ConflictException("Заявка с id=" + r.getId() +
                        " должна находиться в статусе PENDING, текущий статус: " + r.getStatus()
                );
            }
        });

        Status newStatus = Status.valueOf(dto.getStatus());

        List<Request> confirmedList = List.of();
        List<Request> rejectedList = List.of();

        if (Status.CONFIRMED.equals(newStatus)) {
            // Подтверждаем заявки пока не достигнем лимита
            long available = event.getParticipantLimit() - confirmed;

            confirmedList = requests.stream()
                    .limit(available)
                    .peek(r -> r.setStatus(Status.CONFIRMED))
                    .toList();

            // Остальные автоматически отклоняем
            rejectedList = requests.stream()
                    .skip(available)
                    .peek(r -> r.setStatus(Status.REJECTED))
                    .collect(Collectors.toList());

            // Если лимит исчерпан — отклоняем все ожидающие заявки на это событие
            if (confirmedList.size() == available) {
                requestRepository.findAllByEventId(eventId).stream()
                        .filter(r -> Status.PENDING.equals(r.getStatus()))
                        .forEach(r -> r.setStatus(Status.REJECTED));
            }
        } else {
            rejectedList = requests.stream()
                    .peek(r -> r.setStatus(Status.REJECTED))
                    .collect(Collectors.toList());
        }

        requestRepository.saveAll(requests);

        List<Request> finalConfirmed = confirmedList;
        List<Request> finalRejected = rejectedList;

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(finalConfirmed.stream()
                        .map(mapper::toDto).collect(Collectors.toList()))
                .rejectedRequests(finalRejected.stream()
                        .map(mapper::toDto).collect(Collectors.toList()))
                .build();
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

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> confirmedMap = getConfirmedRequestsMap(events);

        return events.stream()
                .map(e -> EventMapper.toFullDto(
                        e,
                        viewsMap.getOrDefault(e.getId(), 0L),
                        confirmedMap.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventOrThrow(eventId);

        // Применяем изменения
        applyAdminUpdates(event, dto);

        Event saved = eventRepository.save(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);

        return EventMapper.toFullDto(saved, getViews(saved), confirmed);
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
        if (events.isEmpty()) return Map.of();

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        // Один запрос к БД вместо N запросов
        return requestRepository.countConfirmedByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0], // event_id
                        row -> (Long) row[1] // count
                ));
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

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
