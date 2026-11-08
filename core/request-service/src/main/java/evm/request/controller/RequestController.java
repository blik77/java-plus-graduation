package evm.request.controller;

import evm.request.dto.ParticipationRequestDto;
import evm.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService service;

    @GetMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> findById(@PathVariable Long userId) {
        return service.findById(userId);
    }

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto save(
        @PathVariable Long userId,
        @RequestParam(value = "eventId") Long eventId
    ) {
        log.info("POST /users/{}/requests — eventId={}", userId, eventId);

        return service.save(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancel(
        @PathVariable Long userId,
        @PathVariable Long requestId
    ) {
        return service.cancel(userId, requestId);
    }

    @GetMapping("/{userId}/requests/{eventId}/confirmed")
    @ResponseStatus(HttpStatus.OK)
    public boolean hasConfirmedRequest(
            @PathVariable Long userId,
            @PathVariable Long eventId
    ) {
        return service.hasConfirmedRequest(userId, eventId);
    }
}
