-- EBD (Escola Bíblica Dominical): ciclo, turma, matrícula (+histórico), aula,
-- material e presença. Nomenclatura em português (ebd_*) por decisão
-- explícita, ao contrário do precedente mais recente (official_act/
-- serving_area em inglês).
--
-- Turma: sempre pertence a um ciclo (cycle_id obrigatório) e a matrícula é
-- sempre automatrícula do aluno (POST .../enrollments, sem personId) — a
-- distinção "turma fixa" (matrícula só por professor/STAFF) existiu numa
-- versão anterior e foi removida a pedido do time em revisão do PR: toda
-- turma funciona da mesma forma. STAFF/professor também pode incluir aluno
-- manualmente em qualquer turma (POST .../enrollments com personId) — as
-- duas vias coexistem.
-- TEACHER é só um rótulo informativo em ebd_enrollment.role — nenhuma regra
-- de ativação ou permissão depende de existir um professor vinculado.

CREATE TYPE ebd_class_status AS ENUM ('DRAFT', 'ACTIVE', 'CLOSED');
CREATE TYPE ebd_enrollment_role AS ENUM ('STUDENT', 'TEACHER');
CREATE TYPE ebd_lesson_status AS ENUM ('DRAFT', 'PUBLISHED');

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

-- Sem campo de ementa: a pedido do time em revisão do PR, ementa não é uma
-- propriedade própria da turma — quem quiser compartilhar uma sobe como mais
-- um item em "materiais gerais" (ebd_material, lesson_id nulo), sem marca
-- especial que a distinga dos demais.
CREATE TABLE IF NOT EXISTS ebd_class (
    id BIGSERIAL PRIMARY KEY,
    cycle_id BIGINT NOT NULL REFERENCES ebd_cycle(id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status ebd_class_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ebd_class_cycle  ON ebd_class(cycle_id);
CREATE INDEX IF NOT EXISTS idx_ebd_class_status ON ebd_class(status);

-- Matrícula (vínculo pessoa↔turma com papel) — espelha serving_area_member.
-- Sem start_date/end_date: "desde quando" não importa mais pro negócio (dá pra
-- checar presença olhando as aulas), e "até quando" já é coberto pelo
-- histórico (deleted_at em ebd_enrollment_history, no DELETE) — não precisa
-- duplicar isso numa coluna na linha viva.
CREATE TABLE IF NOT EXISTS ebd_enrollment (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES ebd_class(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    role ebd_enrollment_role NOT NULL DEFAULT 'STUDENT',
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
-- serving_area_member_history — deleted_at (só preenchido no DELETE) é o
-- registro de "quando essa pessoa saiu da turma".
CREATE TABLE IF NOT EXISTS ebd_enrollment_history (
    history_id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT REFERENCES ebd_enrollment(id) ON DELETE SET NULL,
    class_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    role ebd_enrollment_role NOT NULL,
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

CREATE TRIGGER trigger_ebd_enrollment_history_delete
    BEFORE DELETE ON ebd_enrollment
    FOR EACH ROW
    EXECUTE FUNCTION delete_ebd_enrollment_history();

-- Sem content nem display_order: descrição + material já bastam pro conteúdo
-- da aula, e a ordem de exibição passa a ser sempre lesson_date (por isso ela
-- é obrigatória — sem um campo de ordem manual, duas aulas sem data não têm
-- posição definida).
CREATE TABLE IF NOT EXISTS ebd_lesson (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES ebd_class(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    lesson_date DATE NOT NULL,
    status ebd_lesson_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ebd_lesson_class ON ebd_lesson(class_id);
CREATE INDEX IF NOT EXISTS idx_ebd_lesson_date  ON ebd_lesson(class_id, lesson_date);

CREATE TRIGGER trigger_ebd_lesson_updated_at
    BEFORE UPDATE ON ebd_lesson
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Material da turma (ementa) OU de uma aula específica — lesson_id nulo =
-- geral da turma, preenchido = específico da aula (mesmo princípio de
-- team_id nulo/preenchido em serving_area_member). file_data é o conteúdo
-- binário em si, guardado como BYTEA na própria linha — decisão explícita do
-- usuário de não usar S3 para isto, diferente do padrão de foto de pessoa
-- (S3Service.uploadFile), que continua intocado. file_size_bytes fica
-- desnormalizado para listagem/exibição sem precisar carregar o BYTEA
-- inteiro (ver EbdMaterialRepository: as consultas de listagem nunca
-- selecionam file_data, só o download).
CREATE TABLE IF NOT EXISTS ebd_material (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES ebd_class(id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES ebd_lesson(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size_bytes BIGINT NOT NULL,
    file_data BYTEA NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ebd_material_class  ON ebd_material(class_id);
CREATE INDEX IF NOT EXISTS idx_ebd_material_lesson ON ebd_material(lesson_id);

-- Presença: vinculada à matrícula (ebd_enrollment), não direto à pessoa, para
-- deixar explícito que só quem está matriculado pode ter presença — carrega
-- turma+pessoa por transitividade. Sem self_reported: quem marcou/alterou já
-- dá pra saber por updated_by (é o próprio aluno ou é STAFF/professor).
CREATE TABLE IF NOT EXISTS ebd_attendance (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES ebd_lesson(id) ON DELETE CASCADE,
    enrollment_id BIGINT NOT NULL REFERENCES ebd_enrollment(id) ON DELETE CASCADE,
    present BOOLEAN NOT NULL DEFAULT TRUE,
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
