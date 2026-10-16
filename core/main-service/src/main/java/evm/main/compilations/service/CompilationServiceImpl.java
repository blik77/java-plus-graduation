package evm.main.compilations.service;

import evm.main.compilations.dto.CompilationDto;
import evm.main.compilations.dto.NewCompilationDto;
import evm.main.compilations.dto.UpdateCompilationRequest;
import evm.main.compilations.mapper.CompilationMapper;
import evm.main.compilations.model.Compilation;
import evm.main.compilations.repository.CompilationRepository;
import evm.main.event.dto.EventShortDto;
import evm.main.event.mapper.EventMapper;
import evm.main.event.model.Event;
import evm.main.event.repository.EventRepository;
import evm.main.exceptions.NotFoundException;
import evm.main.requests.repository.RequestRepositoryJpa;
import evm.stat.client.StatsClient;
import evm.stat.dto.ViewStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepositoryJpa requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {

        Set<Event> events = Optional.ofNullable(dto.getEvents())
            .map(eventRepository::findAllById)
            .map(HashSet::new)
            .orElseGet(HashSet::new);

        Compilation compilation =
                CompilationMapper.toEntity(dto, events);

        Compilation saved = compilationRepository.save(compilation);

        return buildCompilationDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }

        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId,
                                            UpdateCompilationRequest dto) {

        Compilation compilation = getCompilationOrThrow(compId);

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            compilation.setEvents(
                    new HashSet<>(eventRepository.findAllById(dto.getEvents()))
            );
        }

        Compilation saved = compilationRepository.save(compilation);

        return buildCompilationDto(saved);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned,
                                                Integer from,
                                                Integer size) {

        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }

        return compilations.stream()
                .map(this::buildCompilationDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilation(Long compId) {

        return buildCompilationDto(getCompilationOrThrow(compId));
    }

    // ==========================================================
    // private
    // ==========================================================

    private Compilation getCompilationOrThrow(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Подборка с id=" + id + " не найдена"));
    }

    private CompilationDto buildCompilationDto(Compilation compilation) {

        List<Event> events =
                new ArrayList<>(compilation.getEvents());

        Map<Long, Long> viewsMap =
                getViewsMap(events);

        Map<Long, Long> confirmedMap =
                getConfirmedRequestsMap(events);

        List<EventShortDto> eventDtos =
                events.stream()
                        .map(event ->
                                EventMapper.toShortDto(
                                        event,
                                        viewsMap.getOrDefault(event.getId(), 0L),
                                        confirmedMap.getOrDefault(event.getId(), 0L)))
                        .toList();

        return CompilationMapper.toDto(compilation, eventDtos);
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        try {

            List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

            LocalDateTime earliest = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(5));

            List<ViewStatsDto> stats =
                statsClient.getStats(
                    earliest,
                    LocalDateTime.now(),
                    uris,
                    true);

            return stats.stream()
                .collect(Collectors.toMap(
                    s -> Long.parseLong(
                        s.getUri().replace("/events/", "")),
                    ViewStatsDto::getHits));

        } catch (Exception e) {

            log.warn("Не удалось получить статистику: {}",
                e.getMessage());

            return Map.of();
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = events.stream()
                .map(Event::getId)
                .toList();

        return requestRepository.countConfirmedByEventIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
