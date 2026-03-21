package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatusEnum {

    PENDING("Pending"),
    APPROVED("Approved"),
    REVISED("Revised"),
    REJECTED("Rejected");

    private final String desc;
}
