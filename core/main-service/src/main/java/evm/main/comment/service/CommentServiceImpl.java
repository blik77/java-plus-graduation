package evm.main.comment.service;

import evm.main.comment.dto.CommentDto;
import evm.main.comment.dto.NewCommentDto;
import evm.main.comment.mapper.CommentMapper;
import evm.main.comment.model.Comment;
import evm.main.comment.repository.CommentRepository;
import evm.main.event.model.Event;
import evm.main.event.model.EventState;
import evm.main.event.repository.EventRepository;
import evm.main.exceptions.ConflictException;
import evm.main.exceptions.NotFoundException;
import evm.main.users.model.User;
import evm.main.users.repository.UserRepositoryJpa;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepositoryJpa userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllCommentsFiltered(Long eventId, Long authorId, Integer from, Integer size) {

        if (eventId != null && authorId != null) {
            throw new IllegalArgumentException("Можно указать только один параметр: eventId или authorId");
        }

        if (eventId != null) {
            return getAllCommentsByEvent(eventId, from, size);
        }

        if (authorId != null) {
            return getAllCommentsByAuthor(authorId, from, size);
        }

        throw new IllegalArgumentException("Необходимо указать параметр eventId или authorId");
    }

    @Override
    @Transactional
    public void deleteAllCommentsFiltered(Long eventId, Long authorId) {
        if (eventId != null && authorId != null) {
            throw new IllegalArgumentException("Можно указать только один параметр: eventId или authorId");
        }

        if (eventId != null) {
            deleteAllCommentsByEvent(eventId);
            return;
        }

        if (authorId != null) {
            deleteAllCommentsByAuthor(authorId);
            return;
        }

        throw new IllegalArgumentException("Необходимо указать параметр eventId или authorId");
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = findUserOrRaiseException(userId);
        Event event = findEventOrRaiseException(eventId);

        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Комментировать можно только опубликованные события");
        }

        Comment c = CommentMapper.toEntity(dto, author, event);
        c.setPublishedDate(LocalDateTime.now());
        Comment saved = commentRepository.save(c);

        log.info("Пользователь {} добавил комментарий к событию {}", userId, eventId);

        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        findUserOrRaiseException(userId);
        Comment comment = findCommentOrRaiseException(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Удалять можно только свои комментарии");
        }
        commentRepository.deleteById(commentId);
        log.info("Пользователь {} удалил комментарий {}", userId, commentId);
    }

    @Override
    @Transactional
    public void deleteCommentById(Long commentId) {
        Comment comment = findCommentOrRaiseException(commentId);
        commentRepository.delete(comment);
        log.info("Администратор удалил комментарий {}", commentId);
    }

    @Override
    @Transactional
    public void deleteAllCommentsByAuthor(Long authorId) {
        findUserOrRaiseException(authorId);
        commentRepository.deleteAllByAuthorId(authorId);
        log.info("Комментарии автора {} удалены", authorId);
    }

    @Override
    @Transactional
    public void deleteAllCommentsByEvent(Long eventId) {
        findEventOrRaiseException(eventId);
        commentRepository.deleteAllByEventId(eventId);
        log.info("Комментарии к событию {} удалены", eventId);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        findUserOrRaiseException(userId);
        Comment c = findCommentOrRaiseException(commentId);

        if (!c.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Редактировать можно только свои комментарии");
        }

        c.setText(dto.getText());
        c.setEditedOn(LocalDateTime.now());

        Comment saved = commentRepository.save(c);
        log.info("Пользователь {} отредактировал комментарий {}", userId, commentId);
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getComment(Long eventId, Long commentId) {
        findEventOrRaiseException(eventId);
        Comment comment = findCommentOrRaiseException(commentId);

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден для события " + eventId);
        }
        return CommentMapper.toDto(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllCommentsByEvent(Long eventId, Integer from, Integer size) {
        findEventOrRaiseException(eventId);
        Pageable p = PageRequest.of(from / size, size);
        return commentRepository.findAllByEventIdOrderByPublishedDateDesc(eventId, p)
                .stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAllCommentsByAuthor(Long authorId, Integer from, Integer size) {
        findUserOrRaiseException(authorId);
        Pageable p = PageRequest.of(from / size, size);
        return commentRepository.findAllByAuthorIdOrderByPublishedDateDesc(authorId, p)
                .stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    private Comment findCommentOrRaiseException(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id = " + commentId + " не найден!"));
    }

    private Event findEventOrRaiseException(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено!"));
    }

    private User findUserOrRaiseException(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Автор/пользователь с id = " + userId + " не найден!"));
    }

}
