package cn.net.wanzni.ai.translation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 智能翻译助手应用主启动类
 * 
 * 基于Spring Boot 3.x的多语言翻译系统
 * 支持文本翻译、文档翻译、实时对话翻译等功能
 * 
 * @version 1.0.0
 * @since 2024-01-15
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "cn.net.wanzni.ai.translation.repository")
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class TranslationAiAgentApplication {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TranslationAiAgentApplication.class, args);
    }
}