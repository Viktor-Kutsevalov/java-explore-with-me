package ru.practicum.stats.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.exception.BadRequestException;
import ru.practicum.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    @Test
    void hit_shouldReturn201() throws Exception {
        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(LocalDateTime.of(2024, 9, 6, 11, 0, 23))
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void getStats_shouldReturn200WithBody() throws Exception {
        List<ViewStatsDto> expected = List.of(
                ViewStatsDto.builder().app("ewm-main-service").uri("/events/1").hits(5L).build()
        );
        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                any(), anyBoolean())).thenReturn(expected);

        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(5));
    }

    @Test
    void getStats_shouldReturn400_whenStartAfterEnd() throws Exception {
        when(statsService.getStats(any(LocalDateTime.class), any(LocalDateTime.class),
                any(), anyBoolean()))
                .thenThrow(new BadRequestException("Start date must be before end date"));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-12-31 23:59:59")
                        .param("end", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date must be before end date"));
    }

    @Test
    void getStats_shouldReturn400_whenMissingParam() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isBadRequest());
    }
}
