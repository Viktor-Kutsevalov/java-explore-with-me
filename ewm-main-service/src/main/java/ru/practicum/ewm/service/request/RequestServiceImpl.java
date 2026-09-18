package ru.practicum.ewm.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.RequestMapper;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.model.enums.RequestStatus;
import ru.practicum.ewm.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final RequestCommonService common;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User requester = common.getUserOrThrow(userId);
        Event event = common.getEventOrThrow(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add a request to own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in unpublished event");
        }
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Request already exists");
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        RequestStatus status;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        log.debug("Created request: {}", saved);
        return RequestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        common.getUserOrThrow(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        common.getUserOrThrow(userId);
        ParticipationRequest request = common.getRequestOrThrow(requestId);

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request with id=" + requestId + " was not found");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        log.debug("Canceled request: {}", saved);
        return RequestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        common.getUserOrThrow(userId);
        Event event = common.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        common.getUserOrThrow(userId);
        Event event = common.getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(dto.getRequestIds());
        if (requests.size() != dto.getRequestIds().size()) {
            throw new NotFoundException("Some requests were not found");
        }

        for (ParticipationRequest r : requests) {
            if (!r.getEvent().getId().equals(eventId)) {
                throw new NotFoundException("Request with id=" + r.getId() + " was not found for this event");
            }
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        List<ParticipationRequestDto> confirmedDtos = new ArrayList<>();
        List<ParticipationRequestDto> rejectedDtos = new ArrayList<>();

        if (dto.getStatus() == EventRequestStatusUpdateRequest.Status.CONFIRMED) {
            if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }

            int freeSlots = event.getParticipantLimit() > 0
                    ? (int) (event.getParticipantLimit() - confirmed)
                    : Integer.MAX_VALUE;

            for (ParticipationRequest r : requests) {
                if (freeSlots > 0) {
                    r.setStatus(RequestStatus.CONFIRMED);
                    confirmedDtos.add(RequestMapper.toDto(requestRepository.save(r)));
                    freeSlots--;
                } else {
                    r.setStatus(RequestStatus.REJECTED);
                    rejectedDtos.add(RequestMapper.toDto(requestRepository.save(r)));
                }
            }

            if (event.getParticipantLimit() > 0
                    && requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                    >= event.getParticipantLimit()) {
                rejectRemainingPending(eventId, dto.getRequestIds(), rejectedDtos);
            }

        } else {
            for (ParticipationRequest r : requests) {
                r.setStatus(RequestStatus.REJECTED);
                rejectedDtos.add(RequestMapper.toDto(requestRepository.save(r)));
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedDtos)
                .rejectedRequests(rejectedDtos)
                .build();
    }

    private void rejectRemainingPending(Long eventId, List<Long> excludeIds,
                                        List<ParticipationRequestDto> rejectedDtos) {
        List<ParticipationRequest> pending = requestRepository.findAllByEventId(eventId).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .filter(r -> !excludeIds.contains(r.getId()))
                .toList();

        for (ParticipationRequest r : pending) {
            r.setStatus(RequestStatus.REJECTED);
            rejectedDtos.add(RequestMapper.toDto(requestRepository.save(r)));
        }
    }
}