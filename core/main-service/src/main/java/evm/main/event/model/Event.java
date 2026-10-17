package evm.main.event.model;

import evm.main.category.model.Category;
import evm.main.users.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;  // краткое описание

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;    // категория события (много событий под одной категорией)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;   // инициатор события

    @Column(nullable = false, length = 7000)
    private String description; // полное описание

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;    // дата и время проведения события

    @Embedded
    private Location location;  // широта и долгота места проведения мероприятия

    @Column(nullable = false)
    private Boolean paid; // платное ли мероприятие

    @Column(name = "participant_limit", nullable = false)
    @Builder.Default
    private Integer participantLimit = 0; // Лимит участников. 0 - отсутствие ограничения

    @Column(name = "request_moderation", nullable = false)
    @Builder.Default
    private Boolean requestModeration = true; // Нужна ли предварительная модерация заявок на участие

    @Column(nullable = false, length = 120)
    private String title;   // заголовок события

    @Enumerated(EnumType.STRING) // хранить строку "PENDING" вместо числа
    @Column(nullable = false)
    @Builder.Default
    private EventState state = EventState.PENDING;  //текущий статус события

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;    // дата создания события

    @Column(name = "published_on")
    private LocalDateTime publishedOn;    // дата публикации события
}
