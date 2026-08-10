package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Filtros opcionais de equipe. Null retorna todas.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaTeamQuery(
        Long id,
        Long servingAreaId
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long servingAreaId;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder servingAreaId(Long v) { this.servingAreaId = v; return this; }

        public ServingAreaTeamQuery build() {
            return new ServingAreaTeamQuery(id, servingAreaId);
        }
    }
}
