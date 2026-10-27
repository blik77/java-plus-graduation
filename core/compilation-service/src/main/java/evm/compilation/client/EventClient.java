package evm.compilation.client;

import evm.compilation.client.dto.EventShortDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "event-service", path = "/internal/events", fallback = EventClientFallback.class)
public interface EventClient {

    @Cacheable(value = "events_batch", key = "#eventIds.toString()")
    @GetMapping
    List<EventShortDto> getEventsByIds(@RequestParam("ids") List<Long> eventIds);


}