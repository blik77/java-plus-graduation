package evm.event.mapper;

import evm.category.mapper.CategoryMapper;
import evm.category.model.Category;
import evm.event.dto.EventFullDto;
import evm.event.dto.EventInternalInfoDto;
import evm.event.dto.EventShortDto;
import evm.event.dto.NewEventDto;
import evm.event.model.Event;
import evm.event.model.EventState;

import java.time.LocalDateTime;

public class EventMapper {

    private EventMapper() {}

    public static Event toEntity(NewEventDto dto, Category category, Long initiatorId) {
        return Event.builder()
            .annotation(dto.getAnnotation())
            .category(category)
            .description(dto.getDescription())
            .eventDate(dto.getEventDate())
            .initiatorId(initiatorId)
            .location(LocationMapper.toEntity(dto.getLocation()))
            .paid(dto.getPaid())
            .participantLimit(dto.getParticipantLimit())
            .requestModeration(dto.getRequestModeration())
            .title(dto.getTitle())
            .state(EventState.PENDING)
            .createdOn(LocalDateTime.now())
            .build();
    }

    public static EventShortDto toShortDto(Event event, Long views, Long confirmedRequests, String initiatorName) {
        return EventShortDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(CategoryMapper.toDto(event.getCategory()))
            .confirmedRequests(confirmedRequests)
            .eventDate(event.getEventDate())
            .initiator(mapToShortUserDto(event.getInitiatorId(), initiatorName))
            .paid(event.getPaid())
            .title(event.getTitle())
            .views(views)
            .build();
    }

    public static EventFullDto toFullDto(Event event, Long views, Long confirmedRequests, String initiatorName) {
        return EventFullDto.builder()
            .id(event.getId())
            .annotation(event.getAnnotation())
            .category(CategoryMapper.toDto(event.getCategory()))
            .confirmedRequests(confirmedRequests)
            .createdOn(event.getCreatedOn())
            .description(event.getDescription())
            .eventDate(event.getEventDate())
            .initiator(mapToFullUserDto(event.getInitiatorId(), initiatorName))
            .location(LocationMapper.toDto(event.getLocation()))
            .paid(event.getPaid())
            .participantLimit(event.getParticipantLimit())
            .publishedOn(event.getPublishedOn())
            .requestModeration(event.getRequestModeration())
            .state(event.getState())
            .title(event.getTitle())
            .views(views)
            .build();
    }

    private static EventShortDto.UserShortDto mapToShortUserDto(Long initiatorId, String name) {
        EventShortDto.UserShortDto dto = new EventShortDto.UserShortDto();
        dto.setId(initiatorId);
        dto.setName(name);
        return dto;
    }

    private static EventFullDto.UserShortDto mapToFullUserDto(Long initiatorId, String name) {
        EventFullDto.UserShortDto dto = new EventFullDto.UserShortDto();
        dto.setId(initiatorId);
        dto.setName(name);
        return dto;
    }

    public static EventInternalInfoDto toInternalInfoDto(Event event, String initiatorName) {
        EventInternalInfoDto dto = new EventInternalInfoDto();
        dto.setId(event.getId());

        EventInternalInfoDto.InitiatorDto initiator = new EventInternalInfoDto.InitiatorDto();
        initiator.setId(event.getInitiatorId());
        initiator.setName(initiatorName != null ? initiatorName : "Unknown");
        dto.setInitiator(initiator);

        dto.setState(event.getState().name());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setPublishedOn(event.getPublishedOn());

        return dto;
    }

}