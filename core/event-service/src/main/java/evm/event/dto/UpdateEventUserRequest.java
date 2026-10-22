package evm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Данные для изменения информации о событии пользователем
// Если поле в запросе не указано (равно null) - значит изменение этих данных не требуется
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {

    @Size(min = 20, max = 2000)
    private String annotation;  //новая аннотация

    private Long category;  // новая категория

    @Size(min = 20, max = 7000)
    private String description; // новое описание

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;    // новые дата и время на которые намечено событие

    private LocationDto location;   // широта и долгота места проведения события

    private Boolean paid;   // Новое значение флага о платности мероприятия

    @PositiveOrZero
    private Integer participantLimit;   //Новый лимит пользователей

    private Boolean requestModeration;  //Нужна ли пре-модерация заявок на участие

    // SEND_TO_REVIEW — отправить на модерацию
    // CANCEL_REVIEW  — отменить событие
    private String stateAction; //Изменение состояния события

    @Size(min = 3, max = 120)
    private String title;   //Новый заголовок
}