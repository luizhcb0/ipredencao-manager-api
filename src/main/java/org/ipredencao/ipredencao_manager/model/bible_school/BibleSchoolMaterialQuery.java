package org.ipredencao.ipredencao_manager.model.bible_school;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// generalOnly=true → lesson_id IS NULL (materiais gerais da turma).
@JsonIgnoreProperties(ignoreUnknown = true)
public record BibleSchoolMaterialQuery(
        Long id,
        Long classId,
        Long lessonId,
        Boolean generalOnly
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Long classId;
        private Long lessonId;
        private Boolean generalOnly;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder classId(Long v) { this.classId = v; return this; }
        public Builder lessonId(Long v) { this.lessonId = v; return this; }
        public Builder generalOnly(Boolean v) { this.generalOnly = v; return this; }

        public BibleSchoolMaterialQuery build() {
            return new BibleSchoolMaterialQuery(id, classId, lessonId, generalOnly);
        }
    }
}
