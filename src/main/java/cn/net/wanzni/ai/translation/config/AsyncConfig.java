package cn.net.wanzni.ai.translation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(ChatAsyncProperties.class)
public class AsyncConfig {

    /**
     * 专用于聊天翻译 SSE/自动回复的受控线程池。
     * - 有界队列，避免内存膨胀
     * - CallerRuns 拒绝策略，防止任务被直接丢弃
     */
    @Bean(name = "chatSseExecutor")
    public Executor chatSseExecutor(ChatAsyncProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getCorePoolSize());
        executor.setMaxPoolSize(props.getMaxPoolSize());
        executor.setQueueCapacity(props.getQueueCapacity());
        executor.setKeepAliveSeconds(props.getKeepAliveSeconds());
        executor.setThreadNamePrefix(props.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(props.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(props.getAwaitTerminationSeconds());
        executor.setAllowCoreThreadTimeOut(props.isAllowCoreThreadTimeOut());
        executor.initialize();
        return executor;
    }
}