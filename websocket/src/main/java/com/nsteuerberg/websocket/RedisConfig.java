package com.nsteuerberg.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.annotation.EnableRedisListeners;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.messaging.RedisMessageSendingTemplate;

@Configuration
@EnableRedisListeners
public class RedisConfig {

    @Bean
    public RedisMessageSendingTemplate redisMessageSendingTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        return new RedisMessageSendingTemplate(redisTemplate);
    }
}
