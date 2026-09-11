-- Escola Dominical: ciclo, turma, matrícula (+histórico), aula, material e presença.
-- Material em BYTEA. Só um ciclo ativo por vez.

CREATE TYPE bible_school_class_status AS ENUM ('DRAFT', 'ACTIVE', 'CLOSED');
CREATE TYPE bible_school_enrollment_role AS ENUM ('STUDENT', 'TEACHER');
CREATE TYPE bible_school_lesson_status AS ENUM ('DRAFT', 'PUBLISHED');

CREATE TABLE IF NOT EXISTS bible_school_cycle (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bible_school_cycle_single_active
    ON bible_school_cycle ((active)) WHERE active;

CREATE TABLE IF NOT EXISTS bible_school_class (
    id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES bible_school_cycle(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status bible_school_class_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_bible_school_class_cycle  ON bible_school_class(cycle_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_class_status ON bible_school_class(status);

CREATE TABLE IF NOT EXISTS bible_school_enrollment (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES bible_school_class(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    role bible_school_enrollment_role NOT NULL DEFAULT 'STUDENT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bible_school_enrollment
    ON bible_school_enrollment (class_id, person_id, role);
CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_class  ON bible_school_enrollment(class_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_person ON bible_school_enrollment(person_id);

CREATE TRIGGER trigger_bible_school_cycle_updated_at
    BEFORE UPDATE ON bible_school_cycle
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_bible_school_class_updated_at
    BEFORE UPDATE ON bible_school_class
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_bible_school_enrollment_updated_at
    BEFORE UPDATE ON bible_school_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE IF NOT EXISTS bible_school_enrollment_history (
    history_id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT REFERENCES bible_school_enrollment(id) ON DELETE SET NULL,
    class_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    role bible_school_enrollment_role NOT NULL,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_history_enrollment
    ON bible_school_enrollment_history(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_history_class
    ON bible_school_enrollment_history(class_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_history_person
    ON bible_school_enrollment_history(person_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_enrollment_history_deleted_at
    ON bible_school_enrollment_history(deleted_at DESC);

CREATE OR REPLACE FUNCTION insert_bible_school_enrollment_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO bible_school_enrollment_history (
        enrollment_id,
        class_id,
        person_id,
        role,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        NEW.id,
        NEW.class_id,
        NEW.person_id,
        NEW.role,
        NEW.added_at,
        NEW.updated_at,
        NEW.updated_by,
        NULL
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_bible_school_enrollment_history
    AFTER INSERT OR UPDATE ON bible_school_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION insert_bible_school_enrollment_history();

CREATE OR REPLACE FUNCTION delete_bible_school_enrollment_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO bible_school_enrollment_history (
        enrollment_id,
        class_id,
        person_id,
        role,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        OLD.id,
        OLD.class_id,
        OLD.person_id,
        OLD.role,
        OLD.added_at,
        OLD.updated_at,
        OLD.updated_by,
        CURRENT_TIMESTAMP
    );
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_bible_school_enrollment_history_delete
    BEFORE DELETE ON bible_school_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION delete_bible_school_enrollment_history();

-- lesson_date obrigatória: é o único critério de ordem das aulas.
CREATE TABLE IF NOT EXISTS bible_school_lesson (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES bible_school_class(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    lesson_date DATE NOT NULL,
    status bible_school_lesson_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_bible_school_lesson_class ON bible_school_lesson(class_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_lesson_date  ON bible_school_lesson(class_id, lesson_date);

CREATE TRIGGER trigger_bible_school_lesson_updated_at
    BEFORE UPDATE ON bible_school_lesson
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- lesson_id nulo = material geral da turma; preenchido = da aula.
-- file_data BYTEA na própria linha; file_size_bytes desnormalizado para listar sem carregar o BYTEA.
CREATE TABLE IF NOT EXISTS bible_school_material (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES bible_school_class(id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES bible_school_lesson(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size_bytes BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_bible_school_material_class  ON bible_school_material(class_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_material_lesson ON bible_school_material(lesson_id);

-- Presença via matrícula (não direto à pessoa): só quem está matriculado pode ter presença.
CREATE TABLE IF NOT EXISTS bible_school_attendance (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES bible_school_lesson(id) ON DELETE CASCADE,
    enrollment_id BIGINT NOT NULL REFERENCES bible_school_enrollment(id) ON DELETE CASCADE,
    present BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bible_school_attendance ON bible_school_attendance(lesson_id, enrollment_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_attendance_lesson     ON bible_school_attendance(lesson_id);
CREATE INDEX IF NOT EXISTS idx_bible_school_attendance_enrollment ON bible_school_attendance(enrollment_id);

CREATE TRIGGER trigger_bible_school_attendance_updated_at
    BEFORE UPDATE ON bible_school_attendance
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
