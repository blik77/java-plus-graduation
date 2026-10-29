package evm.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @GetMapping("/requests/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);

    @GetMapping("/users/{userId}/requests/{eventId}/confirmed")
    boolean hasConfirmedRequest(@PathVariable Long userId, @PathVariable Long eventId);
}