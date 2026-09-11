package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolLessonQuery(
        Long id,
        Long classId,
        BibleSchoolLessonStatusEnum status
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long classId;
        private BibleSchoolLessonStatusEnum status;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder classId(Long v) { this.classId = v; return this; }
        public Builder status(BibleSchoolLessonStatusEnum v) { this.status = v; return this; }

        public BibleSchoolLessonQuery build() {
            return new BibleSchoolLessonQuery(id, classId, status);
        }
    }
}
