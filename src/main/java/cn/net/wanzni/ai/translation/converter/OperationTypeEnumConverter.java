package cn.net.wanzni.ai.translation.converter;

import cn.net.wanzni.ai.translation.enums.OperationTypeEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OperationTypeEnumConverter extends EnumConverter<OperationTypeEnum> {
    public OperationTypeEnumConverter() {
        super(OperationTypeEnum.class);
    }
}