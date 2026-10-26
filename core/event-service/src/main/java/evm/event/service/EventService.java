package evm.event.service;

import evm.event.dto.*;
import evm.event.dto.params.AdminEventSearchParams;
import evm.event.dto.params.PublicEventSearchParams;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EventService {

    List<EventShortDto> getPublicEvents(PublicEventSearchParams params,
                                        HttpServletRequest request);

    EventFullDto getPublicEventById(Long id, HttpServletRequest request);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventFullDto> getAdminEvents(AdminEventSearchParams params);

    EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsByIds(List<Long> ids);

    // Внутренний метод для получения события по ID без проверки статуса PUBLISHED
    EventInternalInfoDto getEventByIdInternal(Long id);
}
