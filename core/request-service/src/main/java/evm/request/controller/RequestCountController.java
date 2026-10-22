package evm.request.controller;

import evm.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestCountController {

    private final RequestService requestService;

    @GetMapping("/confirmed-counts")
    public Map<Long, Long> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.getConfirmedRequestsCounts(eventIds);
    }
}