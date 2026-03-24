package cn.net.wanzni.ai.translation.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskConnectedEvent {

    private Long taskId;

    private String taskNo;

    private String traceId;
}
