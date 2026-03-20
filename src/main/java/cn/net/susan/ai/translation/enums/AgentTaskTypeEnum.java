package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentTaskTypeEnum {

    TEXT("Text task"),
    DOCUMENT("Document task");

    private final String desc;
}
