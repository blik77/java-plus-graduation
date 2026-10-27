package evm.comment.repository;

import evm.comment.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByEventIdOrderByCreatedDesc(Long eventId, Pageable pageable);
    List<Comment> findAllByAuthorIdOrderByCreatedDesc(Long authorId, Pageable pageable);
    void deleteAllByAuthorId(Long authorId);
    void deleteAllByEventId(Long eventId);
}