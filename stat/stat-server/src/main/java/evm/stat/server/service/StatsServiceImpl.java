package evm.stat.server.service;

import evm.stat.dto.HitDto;
import evm.stat.dto.ViewStatsDto;
import evm.stat.server.mapper.StatsMapper;
import evm.stat.server.model.EndpointHit;
import evm.stat.server.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Override
    @Transactional
    public void saveHit(HitDto hitDto) {
        EndpointHit hit = StatsMapper.toEntity(hitDto);
        repository.save(hit);

    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }
        List<String> normalizedUris = (uris == null || uris.isEmpty()) ? null : uris;
        return unique
                ? repository.findUniqueStats(start, end, normalizedUris)
                : repository.findStats(start, end, normalizedUris);
    }
}
