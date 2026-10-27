package evm.comment.controller;

import evm.comment.dto.CommentDto;
import evm.comment.service.CommentService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/comments")
@RequiredArgsConstructor
@Validated
public class AdminCommentController {

    private final CommentService commentService;

    // то, что можно только администратору

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        return commentService.getAllCommentsFiltered(eventId, authorId, from, size);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentById(commentId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long authorId) {

        commentService.deleteAllCommentsFiltered(eventId, authorId);
    }

}
