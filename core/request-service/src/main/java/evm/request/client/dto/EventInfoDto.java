package evm.request.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventInfoDto {
    private Long id;
    private InitiatorDto initiator;

    private String state;
    private Integer participantLimit;
    private Boolean requestModeration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    @Data
    public static class InitiatorDto {
        private Long id;
        private String name;

    }

}