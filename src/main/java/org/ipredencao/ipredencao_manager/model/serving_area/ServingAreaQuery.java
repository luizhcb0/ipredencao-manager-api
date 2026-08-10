package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;

// id filtra uma área específica; os demais são filtros da listagem/busca.
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServingAreaQuery(
        Long id,
        String name,
        Boolean active,
        Long supervisorPersonId,
        Long personId,
        PaginationParameters pagination
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String name;
        private Boolean active;
        private Long supervisorPersonId;
        private Long personId;
        private PaginationParameters pagination;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder active(Boolean v) { this.active = v; return this; }
        public Builder supervisorPersonId(Long v) { this.supervisorPersonId = v; return this; }
        public Builder personId(Long v) { this.personId = v; return this; }
        public Builder pagination(PaginationParameters v) { this.pagination = v; return this; }

        public ServingAreaQuery build() {
            return new ServingAreaQuery(id, name, active, supervisorPersonId, personId, pagination);
        }
    }
}
