package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Filtros do relatório de participação (quem serve / quem não serve).
// status nulo = SERVING; includeDeceased nulo/ausente = false (tratado no service).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParticipationReportQuery(
        List<Long> categoryIds,
        Long servingAreaId,
        Long positionId,
        ServingAreaPositionKindEnum kind,
        Status status,
        Boolean includeDeceased
) {
    public enum Status { SERVING, NOT_SERVING, ALL }
}
