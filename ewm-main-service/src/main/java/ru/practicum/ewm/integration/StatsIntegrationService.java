package ru.practicum.ewm.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsIntegrationService {

    private static final String APP_NAME = "ewm-main-service";
    private static final String EVENT_URI_PREFIX = "/events/";
    private static final LocalDateTime STATS_START = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final StatsClient statsClient;

    public void hit(String ip, String uri) {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            statsClient.hit(dto);
        } catch (Exception e) {
            log.warn("Failed to send hit to stats-server: {}", e.getMessage());
        }
    }

    public Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> EVENT_URI_PREFIX + id)
                .toList();

        LocalDateTime end = LocalDateTime.now().plusYears(1);

        List<ViewStatsDto> stats;
        try {
            stats = statsClient.getStats(STATS_START, end, uris, true);
        } catch (Exception e) {
            log.warn("Failed to fetch stats from stats-server: {}", e.getMessage());
            return Map.of();
        }

        Map<Long, Long> result = new HashMap<>();
        for (ViewStatsDto s : stats) {
            Long eventId = parseEventId(s.getUri());
            if (eventId != null) {
                result.put(eventId, s.getHits());
            }
        }
        return result;
    }

    public Long getViewsForEvent(Long eventId) {
        return getViewsForEvents(List.of(eventId)).getOrDefault(eventId, 0L);
    }

    private Long parseEventId(String uri) {
        if (uri == null || !uri.startsWith(EVENT_URI_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(uri.substring(EVENT_URI_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
