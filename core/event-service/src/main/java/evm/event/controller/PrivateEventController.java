package evm.event.controller;

import evm.event.dto.EventFullDto;
import evm.event.dto.EventShortDto;
import evm.event.dto.NewEventDto;
import evm.event.dto.UpdateEventUserRequest;
import evm.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Приватный API — доступен только авторизованным пользователям
// userId берётся из path
@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class PrivateEventController {

    private final EventService eventService;

    // Список событий пользователя
    @GetMapping
    public List<EventShortDto> getUserEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("GET /users/{}/events", userId);
        return eventService.getUserEvents(userId, from, size);
    }

    // Создать новое событие.
    // Дата события не может быть раньше чем через 2 часа от текущего момента
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto newEventDto) {

        log.info("POST /users/{}/events — title={}", userId, newEventDto.getTitle());
        return eventService.addEvent(userId, newEventDto);
    }

    // Полная информация о своём событии
    @GetMapping("/{eventId}")
    public EventFullDto getUserEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("GET /users/{}/events/{}", userId, eventId);
        return eventService.getUserEventById(userId, eventId);
    }

    // Редактировать своё событие
    // Можно изменить только PENDING или CANCELED события
    // Дата не раньше чем через 2 часа от текущего момента
    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateRequest) {

        log.info("PATCH /users/{}/events/{}", userId, eventId);
        return eventService.updateUserEvent(userId, eventId, updateRequest);
    }

}
