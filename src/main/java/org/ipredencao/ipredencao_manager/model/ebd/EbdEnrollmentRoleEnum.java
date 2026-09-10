package org.ipredencao.ipredencao_manager.model.ebd;

// Papel do vínculo pessoa↔turma. Enum nativo do Postgres (ebd_enrollment_role);
// na wire é string plain ("STUDENT", ...). TEACHER não é um perfil_acesso nem
// uma entidade separada — é só este papel no vínculo (ver docs/EBD_ANALISE_E_PLANO.md B.3).
public enum EbdEnrollmentRoleEnum {
    STUDENT,
    TEACHER
}
