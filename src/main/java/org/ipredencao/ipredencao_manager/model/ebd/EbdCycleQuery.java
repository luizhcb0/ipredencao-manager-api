package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdCycleQuery(
        Long id,
        Boolean active
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Boolean active;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder active(Boolean v) { this.active = v; return this; }

        public EbdCycleQuery build() {
            return new EbdCycleQuery(id, active);
        }
    }
}
