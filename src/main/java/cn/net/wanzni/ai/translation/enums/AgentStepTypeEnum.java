package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentStepTypeEnum {

    CREATED("Task created"),
    PLAN("Task planning"),
    RETRIEVE_TERMINOLOGY("Retrieve terminology"),
    RETRIEVE_MEMORY("Retrieve translation memory"),
    TRANSLATE("Translate"),
    QUALITY_CHECK("Quality check"),
    FINALIZE("Finalize");

    private final String desc;
}
