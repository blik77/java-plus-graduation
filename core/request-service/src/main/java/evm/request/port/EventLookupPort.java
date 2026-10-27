package evm.request.port;

import evm.request.dto.EventInfo;

public interface EventLookupPort {
    EventInfo findById(Long eventId);
}