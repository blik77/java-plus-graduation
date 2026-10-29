package evm.event.controller;

import evm.event.dto.EventFullDto;
import evm.event.dto.EventShortDto;
import evm.event.dto.RecommendedEventDto;
import evm.event.dto.params.PublicEventSearchParams;
import evm.event.service.EventService;
import evm.event.service.StatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Публичный API — доступен всем без авторизации
// Каждый запрос фиксируется в сервисе статистики
@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final StatsService statsService;

    // Список событий с фильтрацией
    // Возвращает только PUBLISHED события
    @GetMapping
    public List<EventShortDto> getEvents(
            // Текстовый поиск по аннотации и описанию (без учёта регистра)
            @RequestParam(required = false) String text,
            // Фильтр по категориям
            @RequestParam(required = false) List<Long> categories,
            // Только платные или бесплатные события
            @RequestParam(required = false) Boolean paid,
            // Диапазон дат — если не указан, берём события позже текущего момента
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            // Только события где ещё есть свободные места
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            // Сортировка: EVENT_DATE или VIEWS
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            // HttpServletRequest нужен, чтобы получить ip и uri для статистики
            HttpServletRequest request
    ) {
        log.info("GET /events — text={}, categories={}, paid={}, sort={}",
                text, categories, paid, sort);

        PublicEventSearchParams searchParams = PublicEventSearchParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return eventService.getPublicEvents(searchParams, request);
    }

    // Подробная информация об опубликованном событии.
    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id,
                                 @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        log.info("GET /events/{} для userId={}", id, userId);

        return eventService.getPublicEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<RecommendedEventDto> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int maxResults) {

        log.info("GET /events/recommendations для userId={}", userId);

        return eventService.getRecommendationsForUser(userId, maxResults);
    }

    @GetMapping("/{id}/similar")
    public List<RecommendedEventDto> getSimilarEvents(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int maxResults) {

        log.info("GET /events/{}/similar для userId={}", id, userId);

        return eventService.getSimilarEvents(id, userId, maxResults);
    }

    /**
     * Лайк мероприятия.
     * Пользователь может лайкать только посещённые им мероприятия.
     */
    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {

        log.info("PUT /events/{}/like для userId={}", eventId, userId);

        eventService.likeEvent(userId, eventId);
    }
}
