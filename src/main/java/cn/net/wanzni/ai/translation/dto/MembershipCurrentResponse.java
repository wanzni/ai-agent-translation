package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipCurrentResponse {
    private boolean active;
    private String type;
    private String typeDesc;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long periodQuota;
    private Long periodUsed;
    private Long remainingQuota;
}
