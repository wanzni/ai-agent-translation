package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentTaskStatusEnum {

    PENDING("Pending"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REVIEW_REQUIRED("Review required"),
    CANCELLED("Cancelled");

    private final String desc;
}
