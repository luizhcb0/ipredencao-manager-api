package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// name é exact-match — duplicidade de ciclo usa find, não exists dedicado.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolCycleQuery(
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

        public BibleSchoolCycleQuery build() {
            return new BibleSchoolCycleQuery(id, excludeId, name, active);
        }
    }
}
