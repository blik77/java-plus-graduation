package evm.main.event.service;

import evm.main.event.dto.*;
import evm.main.requests.dto.EventRequestStatusUpdateRequest;
import evm.main.requests.dto.EventRequestStatusUpdateResult;
import evm.main.requests.dto.ParticipationRequestDto;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    // --- Public ---
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort,
                                        Integer from, Integer size,
                                        HttpServletRequest request);

    EventFullDto getPublicEventById(Long id, HttpServletRequest request);

    // --- Private ---
    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    // --- Admin ---
    List<EventFullDto> getAdminEvents(List<Long> users, List<String> states,
                                      List<Long> categories,
                                      LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                      Integer from, Integer size);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);
}
