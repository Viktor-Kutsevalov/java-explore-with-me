package ru.practicum.ewm.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.UpdateEventAdminRequest;
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

    private final EventRepository eventRepository;
    private final EventCommonService common;

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                          int from, int size) {
        common.parseStates(states); // валидация: если есть неизвестное состояние — 400

        Pageable pageable = common.buildPageable(from, size);
        Page<Event> page = eventRepository.findEventsByAdmin(
                users, states, categories, rangeStart, rangeEnd, pageable);
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
            throw new ConflictException(
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
            throw new ConflictException(
                    "Field: eventDate. Error: дата начала изменяемого события должна быть не ранее чем за "
                            + MIN_HOURS_BEFORE_EVENT_ADMIN + " час от даты публикации");
        }
    }
}
