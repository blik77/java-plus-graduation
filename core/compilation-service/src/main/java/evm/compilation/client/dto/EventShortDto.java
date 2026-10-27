package evm.compilation.client.dto;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private InitiatorDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @Data
    public static class CategoryDto {
        private Long id;
        private String name;
    }

    @Data
    public static class InitiatorDto {
        private Long id;
    }
}