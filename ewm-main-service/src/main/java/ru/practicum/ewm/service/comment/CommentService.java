package ru.practicum.ewm.service.comment;

import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;

import java.util.List;

public interface CommentService {

    List<CommentDto> getEventComments(Long eventId, int from, int size);

    CommentDto create(Long userId, Long eventId, NewCommentDto dto);

    void deleteByAuthor(Long userId, Long commentId);

    void deleteByAdmin(Long commentId);
}
