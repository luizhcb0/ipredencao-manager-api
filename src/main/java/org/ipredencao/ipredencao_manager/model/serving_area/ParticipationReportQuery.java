package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Filtros do relatório de participação (quem serve / quem não serve).
// status nulo = SERVING.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParticipationReportQuery(
        List<Long> categoryIds,
        String campus,
        Long servingAreaId,
        Long positionId,
        ServingAreaPositionKindEnum kind,
        Status status
) {
    public enum Status { SERVING, NOT_SERVING, ALL }
}
