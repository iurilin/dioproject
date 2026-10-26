package com.dio.project.service;

import com.dio.project.dto.ComandoRegistro;
import com.dio.project.dto.TransacaoRequest;
import com.dio.project.dto.TransacaoResponse;
import com.dio.project.exception.RegraNegocioException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class AssistenteService {

    private final ChatClient chatClient;
    private final TransacaoService transacaoService;

    public AssistenteService(ChatClient chatClient, TransacaoService transacaoService) {
        this.chatClient = chatClient;
        this.transacaoService = transacaoService;
    }

    public String responder(String mensagem) {
        ComandoRegistro cmd = extrairComando(mensagem);

        if (cmd == null || !cmd.ehRegistro()) {
            return conversar(mensagem);
        }

        try {
            LocalDate data = (cmd.data() == null || cmd.data().isBlank())
                    ? null : LocalDate.parse(cmd.data().trim());

            TransacaoResponse t = transacaoService.criar(new TransacaoRequest(
                    cmd.descricao(), cmd.valor(), cmd.tipo(), cmd.categoria(), data));

            return "Registrado: %s de R$ %s em %s, categoria %s."
                    .formatted(t.tipo() == com.dio.project.domain.TipoTransacao.DESPESA ? "despesa" : "receita",
                            t.valor(), t.data(), t.categoria());
        } catch (RegraNegocioException | DateTimeParseException | NullPointerException e) {
            return "Não consegui registrar: " + e.getMessage();
        }
    }

    private ComandoRegistro extrairComando(String mensagem) {
        return chatClient.prompt()
                .system(s -> s.param("data_hoje", LocalDate.now().toString()))
                .user(u -> u.text("""
                        Analise a mensagem e extraia os dados.
                        ehRegistro é true apenas se a pessoa informa que gastou, pagou, comprou, recebeu ou ganhou um valor.
                        Converta datas relativas usando a data de hoje, no formato yyyy-MM-dd. Se não houver data, deixe nulo.
                        Mensagem: {mensagem}
                        """).param("mensagem", mensagem))
                .call()
                .entity(ComandoRegistro.class);
    }

    private String conversar(String mensagem) {
        return chatClient.prompt()
                .system(s -> s.param("data_hoje", LocalDate.now().toString()))
                .user(mensagem)
                .call()
                .content();
    }
}
