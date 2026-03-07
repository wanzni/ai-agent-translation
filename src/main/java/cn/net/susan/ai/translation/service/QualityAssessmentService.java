package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.dto.QualityAssessmentRequest;
import cn.net.susan.ai.translation.dto.QualityAssessmentResponse;

/**
 * 翻译质量评估服务接口，提供对翻译结果进行质量评估的功能。
 */
public interface QualityAssessmentService {
    /**
     * 对翻译结果进行质量评估。
     *
     * @param request 质量评估请求，包含源文、译文等信息
     * @return 质量评估结果
     * @throws Exception 评估过程中发生的异常
     */
    QualityAssessmentResponse assess(QualityAssessmentRequest request) throws Exception;
}