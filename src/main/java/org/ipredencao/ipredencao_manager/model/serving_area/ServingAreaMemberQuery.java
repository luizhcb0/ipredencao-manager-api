package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Consulta genérica de vínculos. Todo filtro é opcional; combiná-los cobre os
// casos de uso: um vínculo por id, membros de uma área, vínculos de uma pessoa,
// supervisores e a base do relatório. Encerramento = DELETE (histórico em
// serving_area_member_history).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaMemberQuery(
        Long id,
        Long servingAreaId,
        Long personId,
        Long positionId,
        Long teamId,
        ServingAreaPositionKindEnum kind,
        List<Long> categoryIds,
        String campus
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long servingAreaId;
        private Long personId;
        private Long positionId;
        private Long teamId;
        private ServingAreaPositionKindEnum kind;
        private List<Long> categoryIds;
        private String campus;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder servingAreaId(Long v) { this.servingAreaId = v; return this; }
        public Builder personId(Long v) { this.personId = v; return this; }
        public Builder positionId(Long v) { this.positionId = v; return this; }
        public Builder teamId(Long v) { this.teamId = v; return this; }
        public Builder kind(ServingAreaPositionKindEnum v) { this.kind = v; return this; }
        public Builder categoryIds(List<Long> v) { this.categoryIds = v; return this; }
        public Builder campus(String v) { this.campus = v; return this; }

        public ServingAreaMemberQuery build() {
            return new ServingAreaMemberQuery(id, servingAreaId, personId, positionId, teamId, kind,
                    categoryIds, campus);
        }
    }
}
