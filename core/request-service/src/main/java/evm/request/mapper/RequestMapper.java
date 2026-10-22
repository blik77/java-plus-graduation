package evm.request.mapper;

import evm.request.dto.ParticipationRequestDto;
import evm.request.model.Request;
import evm.request.model.Status;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class RequestMapper {
    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
            .id(request.getId())
            .requester(request.getRequesterId())
            .event(request.getEventId())
            .status(request.getStatus().name())
            .created(request.getCreated())
            .build();
    }

    public Request mapToRequest(Long requesterId, Long eventId) {
        return Request.builder()
            .requesterId(requesterId)
            .eventId(eventId)
            .status(Status.PENDING)
            .created(LocalDateTime.now())
            .build();
    }
}