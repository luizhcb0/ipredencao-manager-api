package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Filtros opcionais de cargo. active = true retorna só cargos ativos (selects
// de novos vínculos); null retorna todos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaPositionQuery(
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

        public ServingAreaPositionQuery build() {
            return new ServingAreaPositionQuery(id, servingAreaId, active);
        }
    }
}
