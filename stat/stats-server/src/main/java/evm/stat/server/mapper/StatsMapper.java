package evm.stat.server.mapper;

import evm.stat.dto.HitDto;
import evm.stat.server.model.EndpointHit;

public class StatsMapper {
    private StatsMapper() {
    }

    public static EndpointHit toEntity(HitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();

    }
}
