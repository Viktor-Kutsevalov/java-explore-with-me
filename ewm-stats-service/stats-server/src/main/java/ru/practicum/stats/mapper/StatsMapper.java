package ru.practicum.stats.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.entity.EndpointHit;

@UtilityClass
public class StatsMapper {

    public EndpointHit toEntity(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
