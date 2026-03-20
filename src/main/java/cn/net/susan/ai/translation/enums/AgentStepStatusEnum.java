package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentStepStatusEnum {

    PENDING("Pending"),
    RUNNING("Running"),
    SUCCESS("Success"),
    FAILED("Failed"),
    SKIPPED("Skipped");

    private final String desc;
}
