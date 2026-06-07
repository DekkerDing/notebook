package io.github.dekkerding.examples.infrastructure.elasticsearch;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Elasticsearch RestTemplate配置
 *
 * @author Notebook RAG Team
 * @since 1.0.0
 */
@Configuration
public class ElasticsearchRestTemplateConfig {

    /**
     * 创建RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .requestFactory(this::requestFactory)
                .build();
    }

    /**
     * 创建请求工厂
     */
    private ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5秒连接超时
        factory.setReadTimeout(60000);    // 60秒读取超时
        return factory;
    }
}
