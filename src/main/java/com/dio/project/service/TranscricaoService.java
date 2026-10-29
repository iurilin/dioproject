package com.dio.project.service;

import com.dio.project.exception.RegraNegocioException;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class TranscricaoService {

    private final OpenAiAudioTranscriptionModel modelo;

    public TranscricaoService(OpenAiAudioTranscriptionModel modelo) {
        this.modelo = modelo;
    }

    public String transcrever(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Envie um arquivo de áudio");
        }
        try {
            String nome = arquivo.getOriginalFilename() != null
                    ? arquivo.getOriginalFilename() : "audio.mp3";

            Resource recurso = new ByteArrayResource(arquivo.getBytes()) {
                @Override
                public String getFilename() {
                    return nome;
                }
            };

            String texto = modelo.transcribe(recurso);
            if (texto == null || texto.isBlank()) {
                throw new RegraNegocioException("Não consegui entender o áudio");
            }
            return texto.trim();
        } catch (IOException e) {
            throw new RegraNegocioException("Não foi possível ler o arquivo de áudio");
        }
    }
}
