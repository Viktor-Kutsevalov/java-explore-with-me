package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.model.Comment;

@UtilityClass
public class CommentMapper {

    public CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEvent().getId())
                .author(toUserDto(comment))
                .created(comment.getCreated())
                .build();
    }

    private UserDto toUserDto(Comment comment) {
        return UserDto.builder()
                .id(comment.getAuthor().getId())
                .email(comment.getAuthor().getEmail())
                .name(comment.getAuthor().getName())
                .build();
    }
}
