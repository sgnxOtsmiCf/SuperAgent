package cn.sgnxotsmicf.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class HttpClientConfig {

    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);
        return factory;
    }

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("ai-model-pool")
                .maxConnections(100)// 最大并发连接数
                .pendingAcquireTimeout(Duration.ofSeconds(10))// 获取连接超时时间
                .maxIdleTime(Duration.ofSeconds(60)) // 连接最大空闲时间（空闲超过则释放）
                .maxLifeTime(Duration.ofMinutes(5)) // 连接最大生命周期（防止长期占用）
                .evictInBackground(Duration.ofSeconds(15)) // 后台定时清理无效连接
                .build();
    }

    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)//60s
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)))
                .responseTimeout(Duration.ofSeconds(120));
    }

    @Bean
    public ReactorClientHttpConnector reactorClientHttpConnector(HttpClient httpClient) {
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    public WebClientCustomizer webClientCustomizer(ReactorClientHttpConnector connector) {
        return webClientBuilder -> webClientBuilder.clientConnector(connector);
    }

    @Bean
    public RestClientCustomizer restClientCustomizer(HttpClient httpClient) {
        return restClientBuilder -> restClientBuilder.requestFactory(
                new ReactorClientHttpRequestFactory(httpClient));
    }
}
