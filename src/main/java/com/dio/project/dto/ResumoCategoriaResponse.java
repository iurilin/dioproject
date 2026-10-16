package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoCategoriaResponse(
        Categoria categoria,
        TipoTransacao tipo,
        LocalDate inicio,
        LocalDate fim,
        BigDecimal total
) {}
