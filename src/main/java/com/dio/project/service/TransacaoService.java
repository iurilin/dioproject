package com.dio.project.service;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import com.dio.project.domain.Transacao;
import com.dio.project.dto.ResumoCategoriaResponse;
import com.dio.project.dto.TransacaoRequest;
import com.dio.project.dto.TransacaoResponse;
import com.dio.project.exception.RegraNegocioException;
import com.dio.project.repository.TransacaoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;

    public TransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TransacaoResponse criar(TransacaoRequest request) {
        LocalDate data = request.data() != null ? request.data() : LocalDate.now();

        if (data.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data da transação não pode ser futura");
        }

        Transacao transacao = new Transacao(
                request.descricao().trim(),
                request.valor(),
                request.tipo(),
                request.categoria(),
                data);

        return TransacaoResponse.de(repository.save(transacao));
    }

    @Transactional(readOnly = true)
    public List<TransacaoResponse> listar(LocalDate inicio, LocalDate fim, Categoria categoria) {
        validarPeriodo(inicio, fim);

        List<Transacao> transacoes = categoria == null
                ? repository.findByDataBetweenOrderByDataDesc(inicio, fim)
                : repository.findByCategoriaAndDataBetweenOrderByDataDesc(categoria, inicio, fim);

        return transacoes.stream().map(TransacaoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ResumoCategoriaResponse totalPorCategoria(Categoria categoria, TipoTransacao tipo,
                                                     LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        var total = repository.somarPorTipoECategoria(tipo, categoria, inicio, fim);
        return new ResumoCategoriaResponse(categoria, tipo, inicio, fim, total);
    }

    @Transactional(readOnly = true)
    public java.math.BigDecimal saldo(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        var receitas = repository.somarPorTipo(TipoTransacao.RECEITA, inicio, fim);
        var despesas = repository.somarPorTipo(TipoTransacao.DESPESA, inicio, fim);
        return receitas.subtract(despesas);
    }

    @Transactional
    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new RegraNegocioException("Transação não encontrada: " + id);
        }
        repository.deleteById(id);
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe a data inicial e a final");
        }
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à final");
        }
    }
}
