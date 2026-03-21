package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum OperationTypeEnum implements BaseEnum<Integer> {

    /**
     * 登录
     */
    LOGIN(1, "登录"),

    /**
     * 登出
     */
    LOGOUT(2, "登出"),

    /**
     * 翻译
     */
    TRANSLATION(3, "翻译"),

    /**
     * 文档上传
     */
    DOCUMENT_UPLOAD(4, "文档上传"),

    /**
     * 文档下载
     */
    DOCUMENT_DOWNLOAD(5, "文档下载"),

    /**
     * 创建术语
     */
    TERMINOLOGY_CREATE(6, "创建术语"),

    /**
     * 更新术语
     */
    TERMINOLOGY_UPDATE(7, "更新术语"),

    /**
     * 删除术语
     */
    TERMINOLOGY_DELETE(8, "删除术语"),

    /**
     * 质量评估
     */
    QUALITY_ASSESSMENT(9, "质量评估"),

    /**
     * 系统配置
     */
    SYSTEM_CONFIG(10, "系统配置"),

    /**
     * 用户管理
     */
    USER_MANAGEMENT(11, "用户管理"),

    /**
     * API调用
     */
    API_CALL(12, "API调用"),

    /**
     * 错误
     */
    ERROR(13, "错误");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}