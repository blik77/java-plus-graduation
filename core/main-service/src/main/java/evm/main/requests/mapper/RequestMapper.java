package evm.main.requests.mapper;

import evm.main.event.model.Event;
import evm.main.requests.dto.ParticipationRequestDto;
import evm.main.requests.model.Request;
import evm.main.requests.model.Status;
import evm.main.users.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RequestMapper {
    private RequestMapper() {
    }

    public Request mapToRequest(User user, Event event) {

        Request request = new Request();
        request.setCreated(LocalDateTime.now());
        request.setStatus(Status.PENDING);
        request.setRequester(user);
        request.setEvent(event);

        return request;

    }

    public ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .created(request.getCreated())
                .status(request.getStatus().name())
                .build();
    }
}
