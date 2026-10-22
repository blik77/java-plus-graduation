package evm.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventInternalInfoDto {
    private Long id;
    private InitiatorDto initiator;
    private String state;
    private Integer participantLimit;
    private Boolean requestModeration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitiatorDto {
        private Long id;
        private String name;
    }
}