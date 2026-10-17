package evm.main.compilations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    @Builder.Default
    private Set<Long> events = new HashSet<>();

    @JsonProperty(defaultValue = "false")
    private Boolean pinned;

    @NotBlank
    @Size(min = 1, max = 50)
    private String title;
}
