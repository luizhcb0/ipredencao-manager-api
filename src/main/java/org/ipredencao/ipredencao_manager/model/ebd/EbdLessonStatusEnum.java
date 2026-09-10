package org.ipredencao.ipredencao_manager.model.ebd;

// Ciclo de vida da aula. Enum nativo do Postgres (ebd_lesson_status); na wire é
// string plain ("DRAFT", ...). Aluno matriculado só enxerga aulas PUBLISHED.
public enum EbdLessonStatusEnum {
    DRAFT,
    PUBLISHED
}
