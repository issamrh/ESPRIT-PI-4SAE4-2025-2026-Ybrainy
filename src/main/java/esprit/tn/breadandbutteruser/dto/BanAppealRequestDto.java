package esprit.tn.breadandbutteruser.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanAppealRequestDto {
    @NotBlank(message = "Description is required")
    private String description;
}
