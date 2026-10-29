package com.dio.project.service;

import com.dio.project.dto.Periodo;
import com.dio.project.exception.RegraNegocioException;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

@Component
public class PeriodoResolver {

    public record Intervalo(LocalDate inicio, LocalDate fim) {}

    public Intervalo resolver(Periodo periodo, String inicio, String fim, LocalDate hoje) {
        Periodo p = periodo != null ? periodo : Periodo.ESTE_MES;

        return switch (p) {
            case HOJE -> new Intervalo(hoje, hoje);
            case ONTEM -> new Intervalo(hoje.minusDays(1), hoje.minusDays(1));
            case ESTA_SEMANA -> new Intervalo(
                    hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), hoje);
            case ESTE_MES -> new Intervalo(
                    hoje.withDayOfMonth(1), hoje.with(TemporalAdjusters.lastDayOfMonth()));
            case MES_PASSADO -> {
                LocalDate mes = hoje.minusMonths(1);
                yield new Intervalo(
                        mes.withDayOfMonth(1), mes.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case ESTE_ANO -> new Intervalo(
                    hoje.withDayOfYear(1), hoje.with(TemporalAdjusters.lastDayOfYear()));
            case PERSONALIZADO -> personalizado(inicio, fim);
        };
    }

    private Intervalo personalizado(String inicio, String fim) {
        if (inicio == null || fim == null || inicio.isBlank() || fim.isBlank()) {
            throw new RegraNegocioException("Informe a data inicial e a final do período");
        }
        try {
            return new Intervalo(LocalDate.parse(inicio.trim()), LocalDate.parse(fim.trim()));
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Datas do período inválidas");
        }
    }
}

