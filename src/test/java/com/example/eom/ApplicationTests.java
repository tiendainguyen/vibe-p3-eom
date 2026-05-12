package com.example.eom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Application.class,
    excludeAutoConfiguration = {
        RedisAutoConfiguration.class,
        RedisReactiveAutoConfiguration.class,
        RabbitAutoConfiguration.class,
        MailSenderAutoConfiguration.class
    }
)
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
