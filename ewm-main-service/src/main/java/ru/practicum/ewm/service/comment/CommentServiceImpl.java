package ru.practicum.ewm.service.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.model.enums.EventState;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.service.event.EventCommonService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventCommonService eventCommon;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        eventCommon.getEventOrThrow(eventId);

        Pageable pageable = buildPageable(from, size);
        Page<Comment> page = commentRepository.findAllByEventId(eventId, pageable);

        return page.getContent().stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto create(Long userId, Long eventId, NewCommentDto dto) {
        User author = eventCommon.getUserOrThrow(userId);
        Event event = eventCommon.getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        Comment comment = Comment.builder()
                .text(dto.getText())
                .event(event)
                .author(author)
                .created(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);
        log.debug("Created comment: {}", saved);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteByAuthor(Long userId, Long commentId) {
        eventCommon.getUserOrThrow(userId);

        Comment comment = commentRepository.findWithAuthorAndEventById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Comment with id=" + commentId + " was not found");
        }

        commentRepository.delete(comment);
        log.debug("Deleted comment id={} by author {}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        Comment comment = commentRepository.findWithAuthorAndEventById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        commentRepository.delete(comment);
        log.debug("Deleted comment id={} by admin", commentId);
    }

    private Pageable buildPageable(int from, int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        return PageRequest.of(from / size, size);
    }
}
