package com.dio.project.service;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import com.dio.project.domain.Transacao;
import com.dio.project.dto.TransacaoRequest;
import com.dio.project.dto.TransacaoResponse;
import com.dio.project.exception.RegraNegocioException;
import com.dio.project.repository.TransacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 10, 31);

    @Mock
    private TransacaoRepository repository;

    @InjectMocks
    private TransacaoService service;

    // ---------- criar ----------

    @Test
    void criarSemDataAssumeHoje() {
        when(repository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));

        TransacaoResponse r = service.criar(new TransacaoRequest(
                "Mercado", new BigDecimal("45.00"), TipoTransacao.DESPESA, Categoria.MERCADO, null));

        assertThat(r.data()).isEqualTo(LocalDate.now());
        assertThat(r.categoria()).isEqualTo(Categoria.MERCADO);
    }

    @Test
    void criarComDataFuturaLancaErroEnaoSalva() {
        var request = new TransacaoRequest("Mercado", new BigDecimal("45.00"),
                TipoTransacao.DESPESA, Categoria.MERCADO, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void criarRemoveEspacosDaDescricao() {
        when(repository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<Transacao> captor = ArgumentCaptor.forClass(Transacao.class);

        service.criar(new TransacaoRequest("  Uber  ", new BigDecimal("30"),
                TipoTransacao.DESPESA, Categoria.TRANSPORTE, null));

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isEqualTo("Uber");
    }

    // ---------- listar ----------

    @Test
    void listarComPeriodoInvertidoLancaErro() {
        assertThatThrownBy(() -> service.listar(FIM, INICIO, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void listarSemCategoriaUsaConsultaPorPeriodo() {
        when(repository.findByDataBetweenOrderByDataDesc(INICIO, FIM)).thenReturn(List.of());

        assertThat(service.listar(INICIO, FIM, null)).isEmpty();

        verify(repository).findByDataBetweenOrderByDataDesc(INICIO, FIM);
        verify(repository, never())
                .findByCategoriaAndDataBetweenOrderByDataDesc(any(), any(), any());
    }

    @Test
    void listarComCategoriaUsaConsultaFiltrada() {
        when(repository.findByCategoriaAndDataBetweenOrderByDataDesc(Categoria.LAZER, INICIO, FIM))
                .thenReturn(List.of());

        service.listar(INICIO, FIM, Categoria.LAZER);

        verify(repository).findByCategoriaAndDataBetweenOrderByDataDesc(Categoria.LAZER, INICIO, FIM);
    }

    // ---------- saldo e totais ----------

    @Test
    void saldoEReceitasMenosDespesas() {
        when(repository.somarPorTipo(TipoTransacao.RECEITA, INICIO, FIM))
                .thenReturn(new BigDecimal("5000.00"));
        when(repository.somarPorTipo(TipoTransacao.DESPESA, INICIO, FIM))
                .thenReturn(new BigDecimal("150.75"));

        assertThat(service.saldo(INICIO, FIM)).isEqualByComparingTo("4849.25");
    }

    @Test
    void saldoNegativoQuandoGastaMaisQueRecebe() {
        when(repository.somarPorTipo(TipoTransacao.RECEITA, INICIO, FIM)).thenReturn(BigDecimal.ZERO);
        when(repository.somarPorTipo(TipoTransacao.DESPESA, INICIO, FIM)).thenReturn(new BigDecimal("80"));

        assertThat(service.saldo(INICIO, FIM)).isEqualByComparingTo("-80");
    }

    @Test
    void totalPorCategoriaDevolveOValorDoRepository() {
        when(repository.somarPorTipoECategoria(TipoTransacao.DESPESA, Categoria.MERCADO, INICIO, FIM))
                .thenReturn(new BigDecimal("45.00"));

        var r = service.totalPorCategoria(Categoria.MERCADO, TipoTransacao.DESPESA, INICIO, FIM);

        assertThat(r.total()).isEqualByComparingTo("45.00");
        assertThat(r.categoria()).isEqualTo(Categoria.MERCADO);
    }

    @Test
    void totalComPeriodoSemDatasLancaErro() {
        assertThatThrownBy(() ->
                service.totalPorCategoria(Categoria.MERCADO, TipoTransacao.DESPESA, null, FIM))
                .isInstanceOf(RegraNegocioException.class);
    }

    // ---------- remover ----------

    @Test
    void removerInexistenteLancaErro() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.remover(99L))
                .isInstanceOf(RegraNegocioException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void removerExistenteApagaPeloId() {
        when(repository.existsById(1L)).thenReturn(true);

        service.remover(1L);

        verify(repository).deleteById(1L);
    }
}
