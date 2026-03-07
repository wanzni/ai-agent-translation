package cn.net.susan.ai.translation.converter;

import cn.net.susan.ai.translation.enums.OperationTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OperationTypeEnumConverter extends EnumConverter<OperationTypeEnum> {
    public OperationTypeEnumConverter() {
        super(OperationTypeEnum.class);
    }
}