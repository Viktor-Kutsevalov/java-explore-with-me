package ru.practicum.stats.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.entity.EndpointHit;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private static final LocalDateTime START = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 12, 31, 23, 59);

    @Test
    void getStats_shouldThrowBadRequest_whenStartIsAfterEnd() {
        assertThatThrownBy(() -> statsService.getStats(END, START, null, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start date must be before end date");

        verifyNoInteractions(statsRepository);
    }

    @Test
    void getStats_shouldCallFindStats_whenUniqueIsFalse() {
        List<ViewStatsDto> expected = List.of(
                ViewStatsDto.builder().app("ewm-main-service").uri("/events/1").hits(5L).build()
        );
        when(statsRepository.findStats(START, END, null)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(START, END, null, false);

        assertThat(result).isEqualTo(expected);
        verify(statsRepository).findStats(START, END, null);
    }

    @Test
    void getStats_shouldCallFindUniqueStats_whenUniqueIsTrue() {
        List<ViewStatsDto> expected = List.of(
                ViewStatsDto.builder().app("ewm-main-service").uri("/events/1").hits(3L).build()
        );
        when(statsRepository.findUniqueStats(START, END, null)).thenReturn(expected);

        List<ViewStatsDto> result = statsService.getStats(START, END, null, true);

        assertThat(result).isEqualTo(expected);
        verify(statsRepository).findUniqueStats(START, END, null);
    }

    @Test
    void getStats_shouldConvertEmptyUrisToNull() {
        when(statsRepository.findStats(eq(START), eq(END), eq(null))).thenReturn(List.of());

        statsService.getStats(START, END, List.of(), false);

        verify(statsRepository).findStats(START, END, null);
    }

    @Test
    void saveHit_shouldPersistEntity() {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(LocalDateTime.of(2024, 9, 6, 11, 0, 23))
                .build();

        statsService.saveHit(dto);

        verify(statsRepository).save(any(EndpointHit.class));
    }
}
