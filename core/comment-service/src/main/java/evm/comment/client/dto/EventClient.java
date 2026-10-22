package evm.comment.client.dto;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", path = "/events")
public interface EventClient {

    @Cacheable(value = "events", key = "#id")
    @GetMapping("/{id}")
    EventShortDto getEvent(@PathVariable("id") Long id);
}