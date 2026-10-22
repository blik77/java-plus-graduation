package evm.request.client;

import evm.request.client.dto.EventInfoDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventClient {

    @Cacheable(value = "events", key = "#id")
    @GetMapping("/internal/events/{id}")
    EventInfoDto getEventInfo(@PathVariable("id") Long id);

}