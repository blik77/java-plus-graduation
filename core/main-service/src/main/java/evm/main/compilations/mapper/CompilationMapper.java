package evm.main.compilations.mapper;

import evm.main.compilations.dto.CompilationDto;
import evm.main.compilations.dto.NewCompilationDto;
import evm.main.compilations.model.Compilation;
import evm.main.event.dto.EventShortDto;
import evm.main.event.model.Event;

import java.util.List;
import java.util.Set;

public class CompilationMapper {

    private CompilationMapper() {
    }

    public static Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .events(events)
                .build();
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(events)
                .build();
    }
}
