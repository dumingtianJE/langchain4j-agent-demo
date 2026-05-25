package com.yourcompany.langchain4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
public class LangChain4jAgentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jAgentDemoApplication.class, args);
    }
}
