package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.dto.compilation.CompilationDto;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.integration.StatsIntegrationService;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.model.enums.RequestStatus;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventConfirmedCount;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventCommonService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsIntegrationService statsIntegrationService;

    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    public Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    public Event getEventOrThrow(Long eventId) {
        return eventRepository.findWithCategoryAndInitiatorById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    public Pageable buildPageable(int from, int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        return PageRequest.of(from / size, size, Sort.by("id").ascending());
    }

    public Pageable buildPageable(int from, int size, Sort sort) {
        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        return PageRequest.of(from / size, size, sort);
    }

    public List<EventState> parseStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }
        try {
            return states.stream().map(EventState::valueOf).toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown state: " + e.getMessage());
        }
    }

    public Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        return statsIntegrationService.getViewsForEvents(eventIds);
    }

    public EventFullDto enrichFull(Event event) {
        EventFullDto dto = EventMapper.toFullDto(event);
        dto.setViews(statsIntegrationService.getViewsForEvent(event.getId()));
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
        return dto;
    }

    public List<EventFullDto> enrichFullList(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> viewsMap = statsIntegrationService.getViewsForEvents(eventIds);
        Map<Long, Long> confirmedMap = countConfirmedMap(eventIds);

        return events.stream()
                .map(e -> {
                    EventFullDto dto = EventMapper.toFullDto(e);
                    dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                    dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    public List<EventShortDto> enrichShortList(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> viewsMap = statsIntegrationService.getViewsForEvents(eventIds);
        return enrichShortList(events, viewsMap);
    }

    public List<EventShortDto> enrichShortList(List<Event> events, Map<Long, Long> viewsMap) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedMap = countConfirmedMap(eventIds);

        return events.stream()
                .map(e -> {
                    EventShortDto dto = EventMapper.toShortDto(e);
                    dto.setViews(viewsMap.getOrDefault(e.getId(), 0L));
                    dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
                    return dto;
                })
                .toList();
    }

    private Map<Long, Long> countConfirmedMap(List<Long> eventIds) {
        return requestRepository.countConfirmedByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventConfirmedCount::getEventId, EventConfirmedCount::getCount));
    }

    public CompilationDto enrichCompilation(Compilation compilation) {
        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(EventMapper::toShortDto)
                .toList();

        if (!eventDtos.isEmpty()) {
            List<Long> eventIds = compilation.getEvents().stream().map(Event::getId).toList();
            Map<Long, Long> viewsMap = statsIntegrationService.getViewsForEvents(eventIds);
            Map<Long, Long> confirmedMap = countConfirmedMap(eventIds);

            eventDtos.forEach(dto -> {
                dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                dto.setConfirmedRequests(confirmedMap.getOrDefault(dto.getId(), 0L));
            });
        }

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }
}
