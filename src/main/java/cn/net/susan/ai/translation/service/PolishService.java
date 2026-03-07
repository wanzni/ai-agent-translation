package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.dto.PolishRequest;
import cn.net.susan.ai.translation.dto.PolishResponse;

/**
 * 润色服务接口，提供对机器翻译（MT）结果进行二次润色的功能。
 */
public interface PolishService {
    /**
     * 根据术语库与风格提示对机器翻译结果进行二次润色。
     *
     * @param request 润色请求，包含待润色文本、术语库、风格提示等
     * @return 润色后的结果
     * @throws Exception 润色过程中发生的异常
     */
    PolishResponse polish(PolishRequest request) throws Exception;
}