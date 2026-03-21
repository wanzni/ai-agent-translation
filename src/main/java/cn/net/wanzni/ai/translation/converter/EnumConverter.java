package cn.net.wanzni.ai.translation.converter;

import cn.net.wanzni.ai.translation.enums.BaseEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 通用枚举转换器基类：将实现 BaseEnum<Integer> 的枚举映射为数据库中的整数值。
 *
 * 说明：JPA 的 AttributeConverter 需要知道具体的枚举类型，因此本类作为抽象基类，
 * 由各枚举的具体 Converter 通过构造传入枚举 Class 进行绑定；统一转换逻辑集中在此类。
 */
@Converter(autoApply = false)
public abstract class EnumConverter<E extends Enum<E> & BaseEnum<Integer>> implements AttributeConverter<E, Integer> {

    private final Class<E> enumClass;

    protected EnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (e.getValue().equals(dbData)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown database value for " + enumClass.getSimpleName() + ": " + dbData);
    }
}