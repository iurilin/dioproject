package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;

import java.math.BigDecimal;

public record ComandoAssistente(
        Intencao intencao,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        Categoria categoria,
        String data,
        Periodo periodo,
        String inicio,
        String fim
) {}
