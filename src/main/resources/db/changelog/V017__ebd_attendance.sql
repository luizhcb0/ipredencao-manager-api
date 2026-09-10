-- EBD — Fase 3: presença. Fecha o plano em docs/EBD_ANALISE_E_PLANO.md.
--
-- Vinculada à matrícula (ebd_enrollment), não direto à pessoa, para deixar
-- explícito que só quem está matriculado pode ter presença — carrega
-- turma+pessoa por transitividade. Vale igual para turma fixa e não-fixa.

CREATE TABLE IF NOT EXISTS ebd_attendance (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES ebd_lesson(id) ON DELETE CASCADE,
    enrollment_id BIGINT NOT NULL REFERENCES ebd_enrollment(id) ON DELETE CASCADE,
    present BOOLEAN NOT NULL DEFAULT TRUE,
    -- FALSE quando um staff/professor registra ou retifica em nome do aluno.
    self_reported BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

-- Uma presença por matrícula por aula.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ebd_attendance ON ebd_attendance(lesson_id, enrollment_id);
CREATE INDEX IF NOT EXISTS idx_ebd_attendance_lesson     ON ebd_attendance(lesson_id);
CREATE INDEX IF NOT EXISTS idx_ebd_attendance_enrollment ON ebd_attendance(enrollment_id);

CREATE TRIGGER trigger_ebd_attendance_updated_at
    BEFORE UPDATE ON ebd_attendance
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
