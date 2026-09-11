package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// name é exact-match (não unaccented-like) — usado pelo service pra checar
// duplicidade de nome (find com name preenchido + excludeId na edição) em vez
// de um existsByName dedicado no repositório.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdCycleQuery(
        Long id,
        Long excludeId,
        String name,
        Boolean active
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long excludeId;
        private String name;
        private Boolean active;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder excludeId(Long v) { this.excludeId = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder active(Boolean v) { this.active = v; return this; }

        public EbdCycleQuery build() {
            return new EbdCycleQuery(id, excludeId, name, active);
        }
    }
}
