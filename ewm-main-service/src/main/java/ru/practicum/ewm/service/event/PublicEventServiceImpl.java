package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {

    private static final String SORT_EVENT_DATE = "EVENT_DATE";
    private static final String SORT_VIEWS = "VIEWS";

    private final EventRepository eventRepository;
    private final EventCommonService common;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort,
                                            int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }

        LocalDateTime effectiveStart = rangeStart != null ? rangeStart : LocalDateTime.now();
        boolean onlyAvail = Boolean.TRUE.equals(onlyAvailable);

        String normalizedSort = sort == null ? SORT_EVENT_DATE : sort;
        if (!SORT_EVENT_DATE.equals(normalizedSort) && !SORT_VIEWS.equals(normalizedSort)) {
            throw new BadRequestException("Unknown sort: " + sort);
        }

        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }

        if (SORT_VIEWS.equals(normalizedSort)) {
            return searchByViews(text, categories, paid, effectiveStart, rangeEnd, onlyAvail, from, size);
        }
        return searchByEventDate(text, categories, paid, effectiveStart, rangeEnd, onlyAvail, from, size);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = common.getEventOrThrow(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        return common.enrichFull(event);
    }


    private List<EventShortDto> searchByEventDate(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime start, LocalDateTime end,
                                                  boolean onlyAvailable, int from, int size) {
        Sort sort = Sort.by("eventDate").ascending();
        Pageable pageable = common.buildPageable(from, size, sort);
        Page<Event> page = eventRepository.findPublishedEvents(
                text, categories, paid, start, end, onlyAvailable, pageable);
        return common.enrichShortList(page.getContent());
    }


    private List<EventShortDto> searchByViews(String text, List<Long> categories, Boolean paid,
                                              LocalDateTime start, LocalDateTime end,
                                              boolean onlyAvailable, int from, int size) {
        Pageable unpaged = Pageable.unpaged(Sort.by("id").ascending());
        Page<Event> all = eventRepository.findPublishedEvents(
                text, categories, paid, start, end, onlyAvailable, unpaged);

        List<Event> events = all.getContent();
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> viewsMap = common.getViewsForEvents(eventIds);

        List<Event> sorted = events.stream()
                .sorted(Comparator.comparingLong(
                        (Event e) -> viewsMap.getOrDefault(e.getId(), 0L)).reversed())
                .toList();

        int startIdx = Math.min(from, sorted.size());
        int endIdx = Math.min(from + size, sorted.size());
        List<Event> pageEvents = sorted.subList(startIdx, endIdx);

        return common.enrichShortList(pageEvents, viewsMap);
    }
}