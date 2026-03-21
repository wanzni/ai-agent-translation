package cn.net.wanzni.ai.translation.converter;

import cn.net.wanzni.ai.translation.enums.ProcessingStatusEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProcessingStatusEnumConverter extends EnumConverter<ProcessingStatusEnum> {
    public ProcessingStatusEnumConverter() {
        super(ProcessingStatusEnum.class);
    }
}