package evm.stat.config;

import evm.stat.client.StatsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class StatsClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate statsRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(500L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        RetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(2);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public StatsClient statsClient(
        DiscoveryClient discoveryClient,
        RetryTemplate statsRetryTemplate,
        @Value("${stats-service.id:stats-server}") String statsServiceId) {
        return new StatsClient(discoveryClient, statsRetryTemplate, statsServiceId);
    }
}