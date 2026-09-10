package org.ipredencao.ipredencao_manager.model.ebd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// generalOnly=true filtra lesson_id IS NULL (materiais gerais da turma/ementa);
// lessonId filtra os de uma aula específica. Os dois nunca são usados juntos.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EbdMaterialQuery(
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

        public EbdMaterialQuery build() {
            return new EbdMaterialQuery(id, classId, lessonId, generalOnly);
        }
    }
}
