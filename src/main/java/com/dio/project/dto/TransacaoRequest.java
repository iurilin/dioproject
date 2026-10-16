package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequest(
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
        String descricao,

        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 13, fraction = 2, message = "Valor inválido")
        BigDecimal valor,

        @NotNull(message = "O tipo é obrigatório")
        TipoTransacao tipo,

        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        // opcional: se nulo, o service assume a data de hoje
        @PastOrPresent(message = "A data não pode ser futura")
        LocalDate data
) {}
