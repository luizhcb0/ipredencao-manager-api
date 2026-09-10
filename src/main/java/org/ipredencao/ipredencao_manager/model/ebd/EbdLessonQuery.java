package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdLessonQuery(
        Long id,
        Long classId,
        EbdLessonStatusEnum status
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long classId;
        private EbdLessonStatusEnum status;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder classId(Long v) { this.classId = v; return this; }
        public Builder status(EbdLessonStatusEnum v) { this.status = v; return this; }

        public EbdLessonQuery build() {
            return new EbdLessonQuery(id, classId, status);
        }
    }
}
