package evm.event.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable //поля встраиваются в таблицу events через @Embedded в Event
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Координаты места проведения
public class Location {
    private Float lat;  // широта
    private Float lon;  // долгота
}
