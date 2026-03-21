package cn.net.wanzni.ai.translation.converter;

import cn.net.wanzni.ai.translation.enums.MembershipTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MembershipTypeEnumConverter extends EnumConverter<MembershipTypeEnum> {
    public MembershipTypeEnumConverter() {
        super(MembershipTypeEnum.class);
    }
}