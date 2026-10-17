package evm.main.comment.mapper;

import evm.main.comment.dto.CommentDto;
import evm.main.comment.dto.NewCommentDto;
import evm.main.comment.model.Comment;
import evm.main.event.model.Event;
import evm.main.users.model.User;

public class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toEntity(NewCommentDto dto, User author, Event event) {
        return Comment.builder()
                .text(dto.getText())
                .event(event)
                .author(author)
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText().trim())
                .eventId(comment.getEvent().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getName())
                .publishedDate(comment.getPublishedDate())
                .editedOn(comment.getEditedOn())
                .build();
    }
}
