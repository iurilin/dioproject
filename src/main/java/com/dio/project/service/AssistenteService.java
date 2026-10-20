package com.dio.project.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AssistenteService {

    private final ChatClient chatClient;

    public AssistenteService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String responder(String mensagem) {
        return chatClient.prompt()
                .system(s -> s.param("data_hoje", LocalDate.now().toString()))
                .user(mensagem)
                .call()
                .content();
    }
}
