-- EBD (Escola Bíblica Dominical) — Fase 1: núcleo do domínio (ciclo, turma,
-- matrícula). Aulas/materiais (Fase 2) e presença (Fase 3) vêm em migrations
-- futuras. Nomenclatura em português (ebd_*) por decisão explícita, ao
-- contrário do precedente mais recente (official_act/serving_area em inglês).
--
-- Turma "fixa" (fixed = TRUE): matrícula feita pelo professor/STAFF, turma
-- conduzida continuamente pelo professor, cycle_id opcional.
-- Turma "não-fixa" (fixed = FALSE): matrícula feita pelo próprio aluno,
-- sempre pertence a um ciclo (cycle_id obrigatório — validado no service, não
-- por CHECK, pois turma fixa pode ou não ter ciclo).
-- Turma fixa não pode ser ativada sem professor vinculado — validado no
-- service (mesmo princípio de "regra de uso" em serving_area: ver V010).

CREATE TYPE ebd_class_status AS ENUM ('DRAFT', 'ACTIVE', 'CLOSED');
CREATE TYPE ebd_enrollment_role AS ENUM ('STUDENT', 'TEACHER');

CREATE TABLE IF NOT EXISTS ebd_cycle (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

-- Só um ciclo ativo por vez (decisão confirmada).
CREATE UNIQUE INDEX IF NOT EXISTS uq_ebd_cycle_single_active
    ON ebd_cycle ((active)) WHERE active;

CREATE TABLE IF NOT EXISTS ebd_class (
    id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT REFERENCES ebd_cycle(id) ON DELETE RESTRICT,
    fixed BOOLEAN NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    syllabus TEXT,
    status ebd_class_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ebd_class_cycle  ON ebd_class(cycle_id);
CREATE INDEX IF NOT EXISTS idx_ebd_class_status ON ebd_class(status);

-- Matrícula (vínculo pessoa↔turma com papel) — espelha serving_area_member.
-- Serve para os dois tipos de turma; quem tem permissão de criar a linha
-- (professor/STAFF vs. o próprio aluno) é decidido no service conforme
-- ebd_class.fixed, não pela forma da tabela.
CREATE TABLE IF NOT EXISTS ebd_enrollment (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES ebd_class(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    role ebd_enrollment_role NOT NULL DEFAULT 'STUDENT',
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ebd_enrollment
    ON ebd_enrollment (class_id, person_id, role);
CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_class  ON ebd_enrollment(class_id);
CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_person ON ebd_enrollment(person_id);

CREATE TRIGGER trigger_ebd_cycle_updated_at
    BEFORE UPDATE ON ebd_cycle
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_ebd_class_updated_at
    BEFORE UPDATE ON ebd_class
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_ebd_enrollment_updated_at
    BEFORE UPDATE ON ebd_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Histórico de matrícula: mesmo padrão completo (insert+update+delete) de
-- serving_area_member_history — presença/situação/data de saída (fases
-- futuras) dependem de auditoria de matrícula.
CREATE TABLE IF NOT EXISTS ebd_enrollment_history (
    history_id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT REFERENCES ebd_enrollment(id) ON DELETE SET NULL,
    class_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    role ebd_enrollment_role NOT NULL,
    start_date DATE,
    end_date DATE,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_history_enrollment
    ON ebd_enrollment_history(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_history_class
    ON ebd_enrollment_history(class_id);
CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_history_person
    ON ebd_enrollment_history(person_id);
CREATE INDEX IF NOT EXISTS idx_ebd_enrollment_history_deleted_at
    ON ebd_enrollment_history(deleted_at DESC);

CREATE OR REPLACE FUNCTION insert_ebd_enrollment_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO ebd_enrollment_history (
        enrollment_id,
        class_id,
        person_id,
        role,
        start_date,
        end_date,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        NEW.id,
        NEW.class_id,
        NEW.person_id,
        NEW.role,
        NEW.start_date,
        NEW.end_date,
        NEW.added_at,
        NEW.updated_at,
        NEW.updated_by,
        NULL
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_ebd_enrollment_history
    AFTER INSERT OR UPDATE ON ebd_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION insert_ebd_enrollment_history();

CREATE OR REPLACE FUNCTION delete_ebd_enrollment_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO ebd_enrollment_history (
        enrollment_id,
        class_id,
        person_id,
        role,
        start_date,
        end_date,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        OLD.id,
        OLD.class_id,
        OLD.person_id,
        OLD.role,
        OLD.start_date,
        OLD.end_date,
        OLD.added_at,
        OLD.updated_at,
        OLD.updated_by,
        CURRENT_TIMESTAMP
    );
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_ebd_enrollment_history_delete
    BEFORE DELETE ON ebd_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION delete_ebd_enrollment_history();
