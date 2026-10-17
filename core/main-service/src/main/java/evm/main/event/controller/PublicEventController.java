package evm.main.event.controller;

import evm.main.event.dto.EventFullDto;
import evm.main.event.dto.EventShortDto;
import evm.main.event.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            HttpServletRequest request) {

        log.info("GET /events — text={}, categories={}, paid={}, sort={}",
                text, categories, paid, sort);

        return eventService.getPublicEvents(
                text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, request);
    }

    // Подробная информация об опубликованном событии.
    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id,
                                 HttpServletRequest request) {
        log.info("GET /events/{}", id);

        return eventService.getPublicEventById(id, request);
    }
}
