package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Filtros opcionais de cargo. Null retorna todos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaPositionQuery(
        Long id,
        Long servingAreaId
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long servingAreaId;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder servingAreaId(Long v) { this.servingAreaId = v; return this; }

        public ServingAreaPositionQuery build() {
            return new ServingAreaPositionQuery(id, servingAreaId);
        }
    }
}
