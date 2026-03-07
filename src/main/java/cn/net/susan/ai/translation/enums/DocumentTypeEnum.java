package cn.net.susan.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档类型枚举
 *
 * @author 苏三
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum DocumentTypeEnum implements BaseEnum<Integer> {

    /**
     * PDF文档
     */
    PDF(1, "PDF文档"),

    /**
     * Word文档(旧版)
     */
    DOC(2, "Word文档(旧版)"),

    /**
     * Word文档
     */
    DOCX(3, "Word文档"),

    /**
     * 文本文件
     */
    TXT(4, "文本文件"),

    /**
     * Excel表格(旧版)
     */
    XLS(5, "Excel表格(旧版)"),

    /**
     * Excel表格
     */
    XLSX(6, "Excel表格"),

    /**
     * PowerPoint演示文稿(旧版)
     */
    PPT(7, "PowerPoint演示文稿(旧版)"),

    /**
     * PowerPoint演示文稿
     */
    PPTX(8, "PowerPoint演示文稿"),

    /**
     * HTML网页
     */
    HTML(9, "HTML网页"),

    /**
     * XML文件
     */
    XML(10, "XML文件"),

    /**
     * JSON文件
     */
    JSON(11, "JSON文件");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;

    /**
     * 根据文件扩展名获取文档类型枚举
     *
     * @param extension 文件扩展名
     * @return 文档类型枚举
     */
    public static DocumentTypeEnum fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        for (DocumentTypeEnum type : values()) {
            if (type.name().equalsIgnoreCase(extension)) {
                return type;
            }
        }
        return null;
    }
}