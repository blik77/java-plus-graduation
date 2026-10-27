package evm.request.service;

import evm.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestService {

    List<ParticipationRequestDto> findById(Long userId);

    ParticipationRequestDto save(Long userId, Long eventId);

    ParticipationRequestDto cancel(Long userId, Long requestId);

    Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds);

}
