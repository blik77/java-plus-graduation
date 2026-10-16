package evm.main.event.controller;

import evm.main.event.dto.EventFullDto;
import evm.main.event.dto.UpdateEventAdminRequest;
import evm.main.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

//Административный API для модерации событий.
@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;

    // Возвращает полную информацию о каждом событии с фильтрацией
    @GetMapping
    public List<EventFullDto> getEvents(
            @RequestParam(required = false) List<Long> users,
            // Фильтр по статусам: PENDING, PUBLISHED, CANCELED
            @RequestParam(required = false) List<String> states,
            // Фильтр по категориям
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("GET /admin/events — users={}, states={}, categories={}",
                users, states, categories);

        return eventService.getAdminEvents(
                users, states, categories, rangeStart, rangeEnd, from, size);
    }

    // Публикация или отклонение события
    // - публиковать можно только PENDING событие
    // - отклонять можно только не опубликованное событие
    // - дата события должна быть не раньше чем через час от публикации
    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest) {

        log.info("PATCH /admin/events/{} — stateAction={}",
                eventId, updateRequest.getStateAction());

        return eventService.updateAdminEvent(eventId, updateRequest);
    }
}
