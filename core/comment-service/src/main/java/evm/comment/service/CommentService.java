package evm.comment.service;

import evm.comment.dto.CommentDto;
import evm.comment.dto.NewCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    void deleteComment(Long userId, Long commentId);

    void deleteCommentById(Long commentId);

    void deleteAllCommentsByAuthor(Long authorId);

    void deleteAllCommentsByEvent(Long eventId);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto);

    CommentDto getComment(Long eventId, Long commentId);

    List<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size);

    List<CommentDto> getAllCommentsByAuthor(Long authorId, Integer from, Integer size);

    List<CommentDto> getAllCommentsFiltered(Long eventId, Long authorId, Integer from, Integer size);

    void deleteAllCommentsFiltered(Long eventId, Long authorId);
}
