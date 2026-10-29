package com.dio.project.controller;

import com.dio.project.dto.VozResponse;
import com.dio.project.service.AssistenteService;
import com.dio.project.service.TranscricaoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voz")
public class VozController {

    private final TranscricaoService transcricaoService;
    private final AssistenteService assistenteService;

    public VozController(TranscricaoService transcricaoService, AssistenteService assistenteService) {
        this.transcricaoService = transcricaoService;
        this.assistenteService = assistenteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VozResponse processar(@RequestParam("audio") MultipartFile audio) {
        String texto = transcricaoService.transcrever(audio);
        String resposta = assistenteService.responder(texto);
        return new VozResponse(texto, resposta);
    }
}
