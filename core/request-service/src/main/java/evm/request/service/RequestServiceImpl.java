package evm.request.service;

import evm.common.exceptions.ConflictException;
import evm.common.exceptions.NotFoundException;
import evm.request.client.EventClient;
import evm.request.client.UserClient;
import evm.request.client.dto.EventInfoDto;
import evm.request.client.dto.UserDto;
import evm.request.dto.ParticipationRequestDto;
import evm.request.mapper.RequestMapper;
import evm.request.model.Request;
import evm.request.model.Status;
import evm.request.repository.RequestRepositoryJpa;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepositoryJpa requestRepository;
    private final RequestMapper mapper;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    public Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = requestRepository.countConfirmedByEventIds(eventIds);

        return results.stream().collect(Collectors.toMap(
            row -> (Long) row[0], // eventId
            row -> (Long) row[1]  // count
        ));
    }

    @Override
    public List<ParticipationRequestDto> findById(Long userId) {
        checkUserExists(userId);
        List<Request> requestsList = requestRepository.findAllByRequesterId(userId);
        return requestsList.isEmpty() ? Collections.emptyList() : requestsList.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto save(Long userId, Long eventId) {
        checkUserExists(userId);

        EventInfoDto event;
        try {
            event = eventClient.getEventInfo(eventId);
        } catch (FeignException e) {
            throw new NotFoundException("Событие с ID " + eventId + " не найдено");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Нельзя отправить повторную заявку на участие в мероприятии.");
        }

        if (event.getInitiator() != null && event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (!"PUBLISHED".equals(event.getState()) || event.getPublishedOn() == null) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        Status status;
        if (!Boolean.TRUE.equals(event.getRequestModeration()) || event.getParticipantLimit() == null || event.getParticipantLimit() == 0) {
            status = Status.CONFIRMED;
        } else {
            status = Status.PENDING;
        }

        if (status == Status.CONFIRMED && event.getParticipantLimit() > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("У события достигнут лимит запросов на участие");
            }
        }

        Request request = mapper.mapToRequest(userId, eventId);
        request.setStatus(status);

        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        checkUserExists(userId);

        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new NotFoundException("Запроса с ID " + requestId + " не найдено"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Заявка с ID " + requestId + " не принадлежит пользователю " + userId);
        }

        request.setStatus(Status.CANCELED);
        return mapper.toDto(requestRepository.save(request));
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