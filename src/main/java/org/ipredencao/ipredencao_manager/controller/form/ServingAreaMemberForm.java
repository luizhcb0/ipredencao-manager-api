package org.ipredencao.ipredencao_manager.controller.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.LocalDate;

// Usado na criação e edição de vínculo (mandato). Encerramento = DELETE do
// vínculo (histórico automático). teamId nulo = escopo geral da área.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaMemberForm(
        Long personId,
        Long positionId,
        Long teamId,
        LocalDate startDate
) {}
