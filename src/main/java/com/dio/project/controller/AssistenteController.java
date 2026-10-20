package com.dio.project.controller;

import com.dio.project.service.AssistenteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistente")
public class AssistenteController {

    private final AssistenteService assistente;

    public AssistenteController(AssistenteService assistente) {
        this.assistente = assistente;
    }

    @PostMapping
    public String perguntar(@RequestBody String mensagem) {
        return assistente.responder(mensagem);
    }
}
