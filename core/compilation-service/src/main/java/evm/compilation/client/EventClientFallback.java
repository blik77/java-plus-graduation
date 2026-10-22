package evm.compilation.client;

import evm.compilation.client.dto.EventShortDto;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> eventIds) {
        return Collections.emptyList();
    }
}