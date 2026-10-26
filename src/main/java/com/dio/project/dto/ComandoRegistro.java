package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;

import java.math.BigDecimal;

public record ComandoRegistro(
        boolean ehRegistro,
        String descricao,
        BigDecimal valor,
        TipoTransacao tipo,
        Categoria categoria,
        String data
) {}
