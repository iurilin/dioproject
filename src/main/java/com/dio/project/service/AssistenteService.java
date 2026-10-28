package com.dio.project.service;

import com.dio.project.domain.TipoTransacao;
import com.dio.project.dto.*;
import com.dio.project.exception.RegraNegocioException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AssistenteService {

    private static final NumberFormat MOEDA =
            NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ChatClient chatClient;
    private final TransacaoService transacaoService;

    public AssistenteService(ChatClient chatClient, TransacaoService transacaoService) {
        this.chatClient = chatClient;
        this.transacaoService = transacaoService;
    }

    public String responder(String mensagem) {
        ComandoAssistente cmd = extrairComando(mensagem);

        if (cmd == null || cmd.intencao() == null || cmd.intencao() == Intencao.OUTRO) {
            return conversar(mensagem);
        }

        try {
            return switch (cmd.intencao()) {
                case REGISTRAR -> registrar(cmd);
                case TOTAL_CATEGORIA -> total(cmd);
                case SALDO -> saldo(cmd);
                case LISTAR -> listar(cmd);
                case OUTRO -> conversar(mensagem);
            };
        } catch (RegraNegocioException e) {
            return "Não consegui concluir: " + e.getMessage();
        } catch (DateTimeParseException | NullPointerException e) {
            return "Não entendi alguns dados do pedido. Pode reformular?";
        }
    }

    private String registrar(ComandoAssistente cmd) {
        LocalDate data = (cmd.data() == null || cmd.data().isBlank())
                ? null : LocalDate.parse(cmd.data().trim());

        TransacaoResponse t = transacaoService.criar(new TransacaoRequest(
                cmd.descricao(), cmd.valor(), cmd.tipo(), cmd.categoria(), data));

        return "Registrado: %s de %s em %s, categoria %s."
                .formatted(t.tipo() == TipoTransacao.DESPESA ? "despesa" : "receita",
                        MOEDA.format(t.valor()), t.data(), t.categoria());
    }

    private String total(ComandoAssistente cmd) {
        if (cmd.categoria() == null) {
            throw new RegraNegocioException("Diga de qual categoria você quer o total");
        }
        TipoTransacao tipo = cmd.tipo() != null ? cmd.tipo() : TipoTransacao.DESPESA;
        LocalDate[] p = resolverPeriodo(cmd);

        ResumoCategoriaResponse r = transacaoService.totalPorCategoria(cmd.categoria(), tipo, p[0], p[1]);

        return "Total de %s em %s (%s a %s): %s."
                .formatted(tipo == TipoTransacao.DESPESA ? "despesas" : "receitas",
                        r.categoria(), p[0], p[1], MOEDA.format(r.total()));
    }

    private String saldo(ComandoAssistente cmd) {
        LocalDate[] p = resolverPeriodo(cmd);
        return "Saldo de %s a %s: %s."
                .formatted(p[0], p[1], MOEDA.format(transacaoService.saldo(p[0], p[1])));
    }

    private String listar(ComandoAssistente cmd) {
        LocalDate[] p = resolverPeriodo(cmd);
        List<TransacaoResponse> lista = transacaoService.listar(p[0], p[1], cmd.categoria());

        if (lista.isEmpty()) {
            return "Nenhuma transação entre %s e %s.".formatted(p[0], p[1]);
        }
        String linhas = lista.stream().limit(10)
                .map(t -> "- %s | %s | %s | %s".formatted(
                        t.data(), t.descricao(), t.categoria(), MOEDA.format(t.valor())))
                .collect(Collectors.joining("\n"));

        return "Transações de %s a %s (até 10 mais recentes):\n%s".formatted(p[0], p[1], linhas);
    }

    private LocalDate[] resolverPeriodo(ComandoAssistente cmd) {
        LocalDate hoje = LocalDate.now();
        Periodo periodo = cmd.periodo() != null ? cmd.periodo() : Periodo.ESTE_MES;

        return switch (periodo) {
            case HOJE -> new LocalDate[]{hoje, hoje};
            case ONTEM -> new LocalDate[]{hoje.minusDays(1), hoje.minusDays(1)};
            case ESTA_SEMANA -> new LocalDate[]{
                    hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), hoje};
            case ESTE_MES -> new LocalDate[]{
                    hoje.withDayOfMonth(1), hoje.with(TemporalAdjusters.lastDayOfMonth())};
            case MES_PASSADO -> {
                LocalDate mes = hoje.minusMonths(1);
                yield new LocalDate[]{
                        mes.withDayOfMonth(1), mes.with(TemporalAdjusters.lastDayOfMonth())};
            }
            case ESTE_ANO -> new LocalDate[]{
                    hoje.withDayOfYear(1), hoje.with(TemporalAdjusters.lastDayOfYear())};
            case PERSONALIZADO -> new LocalDate[]{
                    LocalDate.parse(cmd.inicio().trim()), LocalDate.parse(cmd.fim().trim())};
        };
    }

    private ComandoAssistente extrairComando(String mensagem) {
        return chatClient.prompt()
                .system(s -> s.param("data_hoje", LocalDate.now().toString()))
                .user(u -> u.text("""
                        Analise a mensagem e extraia os dados.

                        intencao:
                        - REGISTRAR: a pessoa informa que gastou, pagou, comprou, recebeu ou ganhou um valor.
                        - TOTAL_CATEGORIA: pergunta quanto gastou ou recebeu em uma categoria.
                        - SALDO: pergunta o saldo.
                        - LISTAR: pede para ver ou listar transações.
                        - OUTRO: qualquer outra coisa.

                        Para REGISTRAR, preencha descricao, valor (positivo), tipo, categoria e data
                        (yyyy-MM-dd, calculada a partir da data de hoje; nulo se não for citada).
                        Para consultas, preencha periodo: HOJE, ONTEM, ESTA_SEMANA, ESTE_MES, MES_PASSADO,
                        ESTE_ANO, ou PERSONALIZADO (nesse caso preencha inicio e fim em yyyy-MM-dd).
                        Se a pessoa não citar período, use ESTE_MES. Preencha categoria só se ela citar uma.
                        Deixe nulo o que não se aplica.

                        Mensagem: {mensagem}
                        """).param("mensagem", mensagem))
                .call()
                .entity(ComandoAssistente.class);
    }

    private String conversar(String mensagem) {
        return chatClient.prompt()
                .system(s -> s.param("data_hoje", LocalDate.now().toString()))
                .user(mensagem)
                .call()
                .content();
    }
}
