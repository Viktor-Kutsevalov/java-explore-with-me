package ru.practicum.ewm.controller.publicapi;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.service.comment.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "0") @Min(0) int from,
                                             @RequestParam(defaultValue = "10") int size) {
        log.info("GET /events/{}/comments: from={}, size={}", eventId, from, size);
        return commentService.getEventComments(eventId, from, size);
    }
}
