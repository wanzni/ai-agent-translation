package cn.net.wanzni.ai.translation.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewTaskReviseRequest {

    @NotBlank(message = "finalText cannot be blank")
    private String finalText;
}
