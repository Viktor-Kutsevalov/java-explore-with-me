package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.entity.EndpointHit;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        EndpointHit hit = StatsMapper.toEntity(endpointHitDto);
        statsRepository.save(hit);
        log.debug("Saved hit: {}", hit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        if (start == null || end == null) {
            throw new BadRequestException("Start and end dates must not be null");
        }
        if (start.isAfter(end)) {
            throw new BadRequestException("Start date must be before end date");
        }
        if (uris != null && uris.isEmpty()) {
            uris = null;
        }
        return unique
                ? statsRepository.findUniqueStats(start, end, uris)
                : statsRepository.findStats(start, end, uris);
    }
}
