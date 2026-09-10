package org.ipredencao.ipredencao_manager.model.ebd;

// Ciclo de vida da turma. Enum nativo do Postgres (ebd_class_status); na wire é
// string plain ("DRAFT", ...). Turma fixa só pode ir para ACTIVE com pelo menos
// um professor vinculado (ver EbdService.requireTeacherIfActivatingFixedClass).
public enum EbdClassStatusEnum {
    DRAFT,
    ACTIVE,
    CLOSED
}
