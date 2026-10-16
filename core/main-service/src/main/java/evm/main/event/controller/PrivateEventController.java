package evm.main.event.controller;

import evm.main.event.dto.EventFullDto;
import evm.main.event.dto.EventShortDto;
import evm.main.event.dto.NewEventDto;
import evm.main.event.dto.UpdateEventUserRequest;
import evm.main.event.service.EventService;
import evm.main.requests.dto.EventRequestStatusUpdateRequest;
import evm.main.requests.dto.EventRequestStatusUpdateResult;
import evm.main.requests.dto.ParticipationRequestDto;
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

    // Заявки на участие в событии
    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return eventService.getEventRequests(userId, eventId);
    }

    // Подтвердить или отклонить заявки
    // Если лимит исчерпан — все оставшиеся заявки автоматически отклоняются
    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest updateRequest) {

        log.info("PATCH /users/{}/events/{}/requests", userId, eventId);
        return eventService.changeRequestStatus(userId, eventId, updateRequest);
    }
}
