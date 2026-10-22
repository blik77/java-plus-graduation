package evm.request.controller;

import evm.request.dto.EventRequestStatusUpdateRequest;
import evm.request.dto.EventRequestStatusUpdateResult;
import evm.request.dto.ParticipationRequestDto;
import evm.request.service.RequestManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class EventRequestsController {

    private final RequestManagementService requestManagementService;

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(
        @PathVariable Long userId, @PathVariable Long eventId) {
        return requestManagementService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(
        @PathVariable Long userId, @PathVariable Long eventId,
        @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        return requestManagementService.changeRequestStatus(userId, eventId, updateRequest);
    }
}