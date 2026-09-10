package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdAttendanceQuery(
        Long id,
        Long lessonId,
        Long enrollmentId
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long lessonId;
        private Long enrollmentId;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder lessonId(Long v) { this.lessonId = v; return this; }
        public Builder enrollmentId(Long v) { this.enrollmentId = v; return this; }

        public EbdAttendanceQuery build() {
            return new EbdAttendanceQuery(id, lessonId, enrollmentId);
        }
    }
}
