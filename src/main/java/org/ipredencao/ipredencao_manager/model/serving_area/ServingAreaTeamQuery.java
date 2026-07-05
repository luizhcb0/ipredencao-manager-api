package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Filtros opcionais de equipe. active = true retorna só equipes ativas; null todas.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaTeamQuery(
        Long id,
        Long servingAreaId,
        Boolean active
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long servingAreaId;
        private Boolean active;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder servingAreaId(Long v) { this.servingAreaId = v; return this; }
        public Builder active(Boolean v) { this.active = v; return this; }

        public ServingAreaTeamQuery build() {
            return new ServingAreaTeamQuery(id, servingAreaId, active);
        }
    }
}
