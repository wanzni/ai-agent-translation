package cn.net.susan.ai.translation.service;

/**
 * 术语库服务接口
 * <p>
 * 定义术语库管理相关的核心业务功能，包括术语的增删改查、
 * 导入导出、搜索匹配等功能
 *
 * @author 苏三
 * @version 1.0.0
 */
public interface TerminologyService extends
        TerminologyCrudService,
        TerminologySearchService,
        TerminologyImportExportService,
        TerminologyStatisticsService,
        TerminologyMaintenanceService {
}