package cn.net.susan.ai.translation.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 术语库导入导出服务接口
 *
 * @author sushan
 * @version 1.0.0
 */
public interface TerminologyImportExportService {

    /**
     * 导入术语库
     *
     * @param file 术语库文件（支持CSV、Excel等格式）
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param category 默认分类
     * @param domain 默认领域
     * @param createdBy 创建者ID
     * @return 导入结果信息
     * @throws Exception 导入过程中的异常
     */
    Map<String, Object> importTerminology(
            MultipartFile file,
            String sourceLanguage,
            String targetLanguage,
            String category,
            String domain,
            String createdBy
    ) throws Exception;

    /**
     * 导出术语库
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param category 分类（可选）
     * @param domain 领域（可选）
     * @param format 导出格式（csv、excel）
     * @return 导出文件的字节数组
     * @throws Exception 导出过程中的异常
     */
    byte[] exportTerminology(
            String sourceLanguage,
            String targetLanguage,
            String category,
            String domain,
            String format
    ) throws Exception;
}