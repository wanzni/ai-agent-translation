package cn.net.wanzni.ai.translation.dto.agent;

import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentTaskDoneEvent {

    private Long taskId;

    private AgentTaskStatusEnum terminalStatus;
}
