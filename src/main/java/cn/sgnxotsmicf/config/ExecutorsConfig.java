package cn.sgnxotsmicf.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: lixiang
 * @CreateDate: 2026/4/27 18:23
 * @Version: 1.0
 * @Description:
 */

@Configuration
public class ExecutorsConfig {

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private static final int CORE_POOL_SIZE = CPU_CORES * 2 + 1;

    private static final int MAX_POOL_SIZE = CPU_CORES * 3 + 1;

    private static final int QUEUE_CAPACITY = 100;

    @Bean
    public Executor superAgentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("SuperAgentExecutors")
                .corePoolSize(CORE_POOL_SIZE)
                .maxPoolSize(MAX_POOL_SIZE)
                .queueCapacity(QUEUE_CAPACITY)
                .keepAlive(Duration.ofSeconds(60))
                .allowCoreThreadTimeOut(false)
                .build();
        //调用者运行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
