package evm.request.service;

import evm.common.exceptions.ConflictException;
import evm.common.exceptions.NotFoundException;
import evm.request.client.EventClient;
import evm.request.client.UserClient;
import evm.request.client.dto.EventInfoDto;
import evm.request.client.dto.UserDto;
import evm.request.dto.EventRequestStatusUpdateRequest;
import evm.request.dto.EventRequestStatusUpdateResult;
import evm.request.dto.ParticipationRequestDto;
import evm.request.mapper.RequestMapper;
import evm.request.model.Request;
import evm.request.model.Status;
import evm.request.repository.RequestRepositoryJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import feign.FeignException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestManagementService {

    private final RequestRepositoryJpa requestRepository;
    private final RequestMapper mapper;
    private final EventClient eventClient;
    private final UserClient userClient;

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        EventInfoDto event = getEventOrThrow(eventId);

        if (event.getInitiator() == null || !event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено или вы не являетесь его инициатором");
        }

        return requestRepository.findAllByEventId(eventId).stream()
            .map(mapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        checkUserExists(userId);
        EventInfoDto event = getEventOrThrow(eventId);

        if (event.getInitiator() == null || !event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено или вы не являетесь его инициатором");
        }

        List<Long> requestedIds = dto.getRequestIds();

        List<Request> requests = requestRepository.findAllByIdInAndEventId(dto.getRequestIds(), eventId);

        if (requests.size() != requestedIds.size()) {
            throw new IllegalArgumentException("Обнаружены ID заявок, не принадлежащих данному событию.");
        }

        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены для события " + eventId);
        }

        // Если модерация отключена или лимит = 0 — подтверждение не требуется
        if (!Boolean.TRUE.equals(event.getRequestModeration()) ||
            event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
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

        if (event.getParticipantLimit() != null && event.getParticipantLimit() > 0 &&
            confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников достигнут");
        }

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
            long available = event.getParticipantLimit() - confirmed;

            confirmedList = requests.stream()
                .limit(available)
                .peek(r -> r.setStatus(Status.CONFIRMED))
                .toList();

            rejectedList = requests.stream()
                .skip(available)
                .peek(r -> r.setStatus(Status.REJECTED))
                .collect(Collectors.toList());

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

    private EventInfoDto getEventOrThrow(Long eventId) {
        try {
            return eventClient.getEventInfo(eventId);
        } catch (FeignException e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено или недоступно");
        }
    }

    private void checkUserExists(Long userId) {
        try {
            List<UserDto> users = userClient.getUsersByIds(List.of(userId));
            if (users == null || users.isEmpty()) {
                throw new NotFoundException("Пользователь с ID " + userId + " не найден");
            }
        } catch (FeignException e) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}