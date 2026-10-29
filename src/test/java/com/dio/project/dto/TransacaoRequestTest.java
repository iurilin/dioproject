package com.dio.project.dto;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TransacaoRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private TransacaoRequest request(String descricao, String valor, LocalDate data) {
        return new TransacaoRequest(descricao,
                valor == null ? null : new BigDecimal(valor),
                TipoTransacao.DESPESA, Categoria.MERCADO, data);
    }

    @Test
    void requestValidoNaoTemViolacoes() {
        assertThat(validator.validate(request("Mercado", "45.90", null))).isEmpty();
    }

    @Test
    void valorZeroEInvalido() {
        assertThat(validator.validate(request("Mercado", "0", null))).isNotEmpty();
    }

    @Test
    void valorNegativoEInvalido() {
        assertThat(validator.validate(request("Mercado", "-10", null))).isNotEmpty();
    }

    @Test
    void valorNuloEInvalido() {
        assertThat(validator.validate(request("Mercado", null, null))).isNotEmpty();
    }

    @Test
    void descricaoEmBrancoEInvalida() {
        assertThat(validator.validate(request("   ", "10", null))).isNotEmpty();
    }

    @Test
    void dataFuturaEInvalida() {
        assertThat(validator.validate(request("Mercado", "10", LocalDate.now().plusDays(1)))).isNotEmpty();
    }
}
