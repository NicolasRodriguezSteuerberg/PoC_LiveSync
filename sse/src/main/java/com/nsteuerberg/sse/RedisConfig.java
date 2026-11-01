package com.nsteuerberg.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.annotation.EnableRedisListeners;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.messaging.RedisMessageSendingTemplate;

@EnableRedisListeners
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageSendingTemplate redisMessageSendingTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        return new RedisMessageSendingTemplate(template);
    }
}
