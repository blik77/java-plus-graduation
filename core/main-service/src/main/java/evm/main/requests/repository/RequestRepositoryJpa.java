package evm.main.requests.repository;

import evm.main.requests.model.Request;
import evm.main.requests.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepositoryJpa extends JpaRepository<Request, Long> {

    @Query("select r from Request as r where r.requester.id = ?1")
    List<Request> findAllByUserId(Long userId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Query("select COUNT(r) from Request AS r where r.event.id = ?1 AND r.status = ?2")
    long countByEventIdAndStatus(Long eventId, Status status);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    // Количество подтверждённых заявок для каждого события из списка
    @Query("SELECT r.event.id, COUNT(r) " +
            "FROM Request r " +
            "WHERE r.event.id IN :eventIds " +
            "AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    // Все заявки на конкретное событие
    List<Request> findAllByEventId(Long eventId);

    // Заявки по списку ID, отфильтрованные по принадлежности к конкретному событию
    @Query("SELECT r " +
            "FROM Request r " +
            "WHERE r.id IN :ids " +
            "AND r.event.id = :eventId")
    List<Request> findAllByIdInAndEventId(@Param("ids") List<Long> ids, @Param("eventId") Long eventId);

}
