package evm.event.client;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        return eventIds.stream()
            .collect(Collectors.toMap(id -> id, id -> 0L));
    }

}