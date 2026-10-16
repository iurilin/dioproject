package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import com.dio.project.domain.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        Categoria categoria,
        LocalDate data
) {
    public static TransacaoResponse de(Transacao t) {
        return new TransacaoResponse(
                t.getId(), t.getDescricao(), t.getValor(),
                t.getTipo(), t.getCategoria(), t.getData());
    }
}
