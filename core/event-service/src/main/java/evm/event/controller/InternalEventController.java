package evm.event.controller;

import evm.event.dto.EventFullDto;
import evm.event.dto.EventInternalInfoDto;
import evm.event.dto.EventShortDto;
import evm.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEventsByIds(@RequestParam("ids") List<Long> ids) {
        return eventService.getEventsByIds(ids);
    }

    @GetMapping("/{id}")
    public EventInternalInfoDto getEventInternal(@PathVariable Long id) {
        // Вызываем метод сервиса, который ищет событие по ID БЕЗ проверки на PUBLISHED.
        return eventService.getEventByIdInternal(id);
    }

}