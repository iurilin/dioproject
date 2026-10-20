package com.dio.project.config;


import org.springframework.core.io.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          @Value("classpath:prompts/system.st") Resource systemPrompt) {
        return builder
                .defaultSystem((org.springframework.core.io.Resource) systemPrompt)
                .build();
    }
}
