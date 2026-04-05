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
    FUSION("Fuse retrieval results"),
    TRANSLATE("Translate"),
    QUALITY_CHECK("Quality check"),
    REVISE("Revise translation"),
    FINALIZE("Finalize");

    private final String desc;
}
