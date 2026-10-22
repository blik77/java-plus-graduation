package evm.request.repository;

import evm.request.model.Request;
import evm.request.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepositoryJpa extends JpaRepository<Request, Long> {

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    Page<Request> findAllByRequesterId(Long requesterId, Pageable pageable);

    List<Request> findAllByRequesterId(Long requesterId);

    List<Request> findAllByEventId(Long eventId);

    Page<Request> findAllByEventIdOrderByCreatedDesc(Long eventId, Pageable pageable);

    long countByEventIdAndStatus(Long eventId, Status status);

    List<Request> findAllByIdInAndEventId(List<Long> ids, Long eventId);

    @Query("SELECT r.eventId, COUNT(r) FROM Request r WHERE r.status = 'CONFIRMED' AND r.eventId IN :eventIds GROUP BY r.eventId")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);
}