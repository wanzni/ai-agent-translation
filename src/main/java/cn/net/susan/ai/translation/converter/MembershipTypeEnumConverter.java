package cn.net.susan.ai.translation.converter;

import cn.net.susan.ai.translation.enums.MembershipTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MembershipTypeEnumConverter extends EnumConverter<MembershipTypeEnum> {
    public MembershipTypeEnumConverter() {
        super(MembershipTypeEnum.class);
    }
}