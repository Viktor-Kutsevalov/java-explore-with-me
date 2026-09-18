package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.dto.event.UpdateEventUserRequest;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.repository.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {

    private static final int MIN_HOURS_BEFORE_EVENT_USER = 2;

    private final EventRepository eventRepository;
    private final EventCommonService common;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        User initiator = common.getUserOrThrow(userId);
        Category category = common.getCategoryOrThrow(dto.getCategory());

        validateEventDate(dto.getEventDate());

        Event event = EventMapper.toEntity(dto, category, initiator);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event saved = eventRepository.save(event);
        log.debug("Created event: {}", saved);
        return common.enrichFull(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        common.getUserOrThrow(userId);
        Pageable pageable = common.buildPageable(from, size);
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);
        return common.enrichShortList(page.getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        common.getUserOrThrow(userId);
        Event event = common.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        return common.enrichFull(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        common.getUserOrThrow(userId);
        Event event = common.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate());
        }

        if (dto.getCategory() != null) {
            Category category = common.getCategoryOrThrow(dto.getCategory());
            event.setCategory(category);
        }

        EventMapper.applyUserUpdate(event, dto);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        log.debug("Updated event by user: {}", saved);
        return common.enrichFull(saved);
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT_USER))) {
            throw new BadRequestException(
                    "Field: eventDate. Error: должно содержать дату, которая не ранее чем через "
                            + MIN_HOURS_BEFORE_EVENT_USER + " часа от текущего момента. Value: " + eventDate);
        }
    }
}
