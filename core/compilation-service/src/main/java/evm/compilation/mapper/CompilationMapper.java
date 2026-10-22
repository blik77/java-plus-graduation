package evm.compilation.mapper;

import evm.compilation.dto.CompilationDto;
import evm.compilation.dto.NewCompilationDto;
import evm.compilation.model.Compilation;
import evm.compilation.client.dto.EventShortDto;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
            .id(compilation.getId())
            .title(compilation.getTitle())
            .pinned(compilation.getPinned())
            .events(events)
            .build();
    }

    public Compilation toEntity(NewCompilationDto dto) {
        Compilation c = new Compilation();
        c.setTitle(dto.getTitle());
        c.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        c.setEventIds(dto.getEvents() != null ? new HashSet<>(dto.getEvents()) : new HashSet<>());
        return c;
    }
}