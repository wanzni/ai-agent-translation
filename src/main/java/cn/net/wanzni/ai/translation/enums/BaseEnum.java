package cn.net.wanzni.ai.translation.enums;

/**
 * 枚举基接口
 *
 * @param <T> 枚举
 */
public interface BaseEnum<T> {

    /**
     * 获取枚举
     *
     * @return 枚举
     */
    T getValue();
}