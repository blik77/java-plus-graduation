package evm.main.requests.service;

import evm.main.event.model.Event;
import evm.main.event.repository.EventRepository;
import evm.main.exceptions.ConflictException;
import evm.main.exceptions.NotFoundException;
import evm.main.requests.dto.ParticipationRequestDto;
import evm.main.requests.mapper.RequestMapper;
import evm.main.requests.model.Request;
import evm.main.requests.model.Status;
import evm.main.requests.repository.RequestRepositoryJpa;
import evm.main.users.model.User;
import evm.main.users.repository.UserRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final UserRepositoryJpa userRepository;
    private final RequestRepositoryJpa requestRepository;
    private final EventRepository eventRepository;
    private final RequestMapper mapper;

    @Override
    public List<ParticipationRequestDto> findById(Long userId) {

        boolean findUser = userRepository.existsById(userId);

        if (!findUser) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        List<Request> requestsList = requestRepository.findAllByUserId(userId);

        if (requestsList.isEmpty()) {
            return Collections.emptyList();
        }

        return requestsList.stream()
            .map(mapper::toDto)
            .toList();

    }

    @Override
    @Transactional
    public ParticipationRequestDto save(Long userId, Long eventId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(
                "Пользователь с ID " + userId + " не найден"
            ));

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException(
                "Мероприятия с ID " + eventId + " не найдено"
            ));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException(
                "Нельзя отправить повторную заявку на участие в мероприятии."
            );
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(
                "Инициатор события не может добавить запрос на участие в своём событии."
            );
        }

        if (event.getPublishedOn() == null) {
            throw new ConflictException(
                "Нельзя участвовать в неопубликованном событии."
            );
        }

        Status status;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            // модерация выключена или лимит = 0 --> подтверждаем автоматически
            status = Status.CONFIRMED;
        } else {
            status = Status.PENDING;
        }

        if (status == Status.CONFIRMED && event.getParticipantLimit() > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(
                eventId,
                Status.CONFIRMED
            );

            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException(
                    "У события достигнут лимит запросов на участие"
                );
            }
        }

        Request request = mapper.mapToRequest(user, event);
        request.setStatus(status);

        return mapper.toDto(requestRepository.save(request));

    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {

        boolean findUser = userRepository.existsById(userId);

        if (!findUser) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Optional<Request> findRequest = requestRepository.findById(requestId);

        if (findRequest.isEmpty()) {
            throw new NotFoundException("Запроса с ID " + requestId + " не найдено");
        }

        if (!findRequest.get().getRequester().getId().equals(userId)) {
            throw new ConflictException(
                "Заявка с ID " + requestId + " не принадлежит пользователю " + userId
            );
        }

        findRequest.get().setStatus(Status.CANCELED);

        Request update = requestRepository.save(findRequest.get());

        return mapper.toDto(update);
    }

}
