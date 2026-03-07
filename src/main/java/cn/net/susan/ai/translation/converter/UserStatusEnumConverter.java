package cn.net.susan.ai.translation.converter;

import cn.net.susan.ai.translation.enums.UserStatusEnum;
import jakarta.persistence.Converter;

/**
 * 通用枚举转换器的用户状态绑定版本。
 * 额外处理：未知值时安全降级为 INACTIVE，避免错误授予访问权限。
 */
@Converter(autoApply = false)
public class UserStatusEnumConverter extends EnumConverter<UserStatusEnum> {

    public UserStatusEnumConverter() {
        super(UserStatusEnum.class);
    }

    @Override
    public UserStatusEnum convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        for (UserStatusEnum e : UserStatusEnum.class.getEnumConstants()) {
            if (e.getValue().equals(dbData)) {
                return e;
            }
        }
        return UserStatusEnum.INACTIVE;
    }
}