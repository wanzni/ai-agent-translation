package cn.net.susan.ai.translation.converter;

import cn.net.susan.ai.translation.enums.ProcessingStatusEnum;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProcessingStatusEnumConverter extends EnumConverter<ProcessingStatusEnum> {
    public ProcessingStatusEnumConverter() {
        super(ProcessingStatusEnum.class);
    }
}