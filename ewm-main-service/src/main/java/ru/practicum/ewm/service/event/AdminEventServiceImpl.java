package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private static final int MIN_HOURS_BEFORE_EVENT_ADMIN = 1;
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final List<Long> EMPTY_LONG = List.of(-1L);
    private static final List<EventState> EMPTY_STATES =
            List.of(EventState.PENDING, EventState.PUBLISHED, EventState.CANCELED);

    private final EventRepository eventRepository;
    private final EventCommonService common;

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                          int from, int size) {
        List<EventState> parsedStates = common.parseStates(states);

        Pageable pageable = common.buildPageable(from, size);
        Page<Event> page = eventRepository.findEventsByAdmin(
                normalizeLongs(users),
                isEmpty(users),
                parsedStates == null ? EMPTY_STATES : parsedStates,
                parsedStates == null || parsedStates.isEmpty(),
                normalizeLongs(categories),
                isEmpty(categories),
                rangeStart != null ? rangeStart : MIN_DATE,
                rangeEnd != null ? rangeEnd : MIN_DATE,
                rangeEnd == null,
                pageable);
        return common.enrichFullList(page.getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest dto) {
        Event event = common.getEventOrThrow(eventId);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT -> publish(event);
                case REJECT_EVENT -> reject(event);
            }
        }

        if (dto.getEventDate() != null) {
            validateEventDateForAdmin(dto.getEventDate());
        }

        if (dto.getCategory() != null) {
            Category category = common.getCategoryOrThrow(dto.getCategory());
            event.setCategory(category);
        }

        EventMapper.applyAdminUpdate(event, dto);

        Event saved = eventRepository.save(event);
        log.debug("Updated event by admin: {}", saved);
        return common.enrichFull(saved);
    }

    private void publish(Event event) {
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException(
                    "Cannot publish the event because it's not in the right state: " + event.getState());
        }
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_ADMIN))) {
            throw new BadRequestException(
                    "Field: eventDate. Error: дата начала изменяемого события должна быть не ранее чем за "
                            + MIN_HOURS_BEFORE_EVENT_ADMIN + " час от даты публикации");
        }
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(LocalDateTime.now());
    }

    private void reject(Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException(
                    "Cannot reject the event because it's not in the right state: " + event.getState());
        }
        event.setState(EventState.CANCELED);
    }

    private void validateEventDateForAdmin(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_ADMIN))) {
            throw new BadRequestException(
                    "Field: eventDate. Error: дата начала изменяемого события должна быть не ранее чем за "
                            + MIN_HOURS_BEFORE_EVENT_ADMIN + " час от даты публикации");
        }
    }

    private List<Long> normalizeLongs(List<Long> list) {
        return isEmpty(list) ? EMPTY_LONG : list;
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
