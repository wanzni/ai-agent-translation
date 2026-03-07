package cn.net.susan.ai.translation.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson序列化与反序列化配置
 *
 * <p>该配置类主要用于自定义Spring Boot应用中Jackson的行为，特别是针对日期和时间类型（如 {@link LocalDateTime}）的格式化处理。
 *
 * <ul>
 *   <li>配置了 {@link LocalDateTime} 的统一序列化和反序列化格式为 "yyyy-MM-dd HH:mm:ss"。</li>
 *   <li>禁用了将日期序列化为时间戳（timestamps）的默认行为，使其更具可读性。</li>
 *   <li>禁用了根据上下文时区调整日期的功能，以确保日期时间处理的一致性。</li>
 * </ul>
 *
 * @author 苏三
 * @version 1.0.0
 * @since 2025-11-21
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义Jackson ObjectMapper
     *
     * <p>通过 {@link Jackson2ObjectMapperBuilderCustomizer} Bean，对自动配置的 {@code ObjectMapper} 进行精细化调整。
     *
     * @return Jackson2ObjectMapperBuilderCustomizer 实例，用于应用自定义配置。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

            builder.modules(javaTimeModule);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        };
    }
}