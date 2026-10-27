package evm.comment.mapper;

import evm.comment.dto.CommentDto;
import evm.comment.dto.NewCommentDto;
import evm.comment.model.Comment;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public CommentDto toDto(Comment comment, String authorName) {
        return CommentDto.builder()
            .id(comment.getId())
            .text(comment.getText())
            .authorId(comment.getAuthorId())
            .authorName(authorName != null ? authorName : "Unknown")
            .eventId(comment.getEventId())
            .publishedDate(comment.getCreated())
            .editedOn(comment.getEdited())
            .build();
    }

    public Comment toEntity(NewCommentDto dto, Long authorId, Long eventId) {
        return Comment.builder()
            .text(dto.getText())
            .authorId(authorId)
            .eventId(eventId)
            .created(LocalDateTime.now())
            .build();
    }
}