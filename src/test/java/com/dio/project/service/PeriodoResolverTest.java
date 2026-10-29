package com.dio.project.service;

import com.dio.project.dto.Periodo;
import com.dio.project.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoResolverTest {

    private final PeriodoResolver resolver = new PeriodoResolver();

    // quinta-feira
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 29);

    @Test
    void esteMesVaiDoPrimeiroAoUltimoDia() {
        var i = resolver.resolver(Periodo.ESTE_MES, null, null, HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void mesPassadoCobreOMesAnteriorInteiro() {
        var i = resolver.resolver(Periodo.MES_PASSADO, null, null, HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void mesPassadoEmDiaFinalDeMesNaoEstoura() {
        // 31/03 menos um mês cai em 28/02: o período deve ser fevereiro inteiro
        var i = resolver.resolver(Periodo.MES_PASSADO, null, null, LocalDate.of(2026, 3, 31));
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void mesPassadoEmJaneiroVoltaParaDezembroDoAnoAnterior() {
        var i = resolver.resolver(Periodo.MES_PASSADO, null, null, LocalDate.of(2026, 1, 15));
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void estaSemanaComecaNaSegundaEAteHoje() {
        var i = resolver.resolver(Periodo.ESTA_SEMANA, null, null, HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(i.fim()).isEqualTo(HOJE);
    }

    @Test
    void hojeEOntem() {
        assertThat(resolver.resolver(Periodo.HOJE, null, null, HOJE).inicio()).isEqualTo(HOJE);
        var ontem = resolver.resolver(Periodo.ONTEM, null, null, HOJE);
        assertThat(ontem.inicio()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(ontem.fim()).isEqualTo(LocalDate.of(2026, 10, 28));
    }

    @Test
    void esteAnoVaiDeJaneiroADezembro() {
        var i = resolver.resolver(Periodo.ESTE_ANO, null, null, HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void periodoNuloAssumeEsteMes() {
        var i = resolver.resolver(null, null, null, HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void personalizadoUsaAsDatasInformadas() {
        var i = resolver.resolver(Periodo.PERSONALIZADO, "2026-09-01", "2026-09-30", HOJE);
        assertThat(i.inicio()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(i.fim()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void personalizadoSemDatasLancaErroDeNegocio() {
        assertThatThrownBy(() -> resolver.resolver(Periodo.PERSONALIZADO, null, null, HOJE))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void personalizadoComDataInvalidaLancaErroDeNegocio() {
        assertThatThrownBy(() -> resolver.resolver(Periodo.PERSONALIZADO, "ontem", "hoje", HOJE))
                .isInstanceOf(RegraNegocioException.class);
    }
}
