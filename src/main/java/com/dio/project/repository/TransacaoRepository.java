package com.dio.project.repository;

import com.dio.project.domain.Categoria;
import com.dio.project.domain.TipoTransacao;
import com.dio.project.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);

    List<Transacao> findByCategoriaAndDataBetweenOrderByDataDesc(
            Categoria categoria, LocalDate inicio, LocalDate fim);

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.tipo = :tipo
              and t.data between :inicio and :fim
            """)
    BigDecimal somarPorTipo(@Param("tipo") TipoTransacao tipo,
                            @Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim);

    @Query("""
            select coalesce(sum(t.valor), 0)
            from Transacao t
            where t.tipo = :tipo
              and t.categoria = :categoria
              and t.data between :inicio and :fim
            """)
    BigDecimal somarPorTipoECategoria(@Param("tipo") TipoTransacao tipo,
                                      @Param("categoria") Categoria categoria,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);
}
