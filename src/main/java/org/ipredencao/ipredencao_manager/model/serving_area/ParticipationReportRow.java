package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.List;

// Uma linha por pessoa; vínculos atuais agregados em memberships.
// memberships vazio = pessoa não está servindo.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipationReportRow(
        Long personId,
        String personName,
        Long categoryId,
        String categoryName,
        List<Membership> memberships
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Membership(
            Long servingAreaId,
            String servingAreaName,
            String positionName,
            ServingAreaPositionKindEnum kind,
            String teamName,
            LocalDate startDate
    ) {}
}
