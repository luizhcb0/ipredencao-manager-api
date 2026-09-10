-- EBD — Fase 2: aulas e materiais. Presença (Fase 3) fica para depois.
--
-- Materiais são guardados como BYTEA na própria tabela (decisão do usuário:
-- não usar S3 para isso) — diferente do padrão de foto de pessoa
-- (S3Service.uploadFile), que continua intacto e não é tocado aqui.

CREATE TYPE ebd_lesson_status AS ENUM ('DRAFT', 'PUBLISHED');

CREATE TABLE IF NOT EXISTS ebd_lesson (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL REFERENCES ebd_class(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content TEXT,
    lesson_date DATE,
    display_order INT NOT NULL DEFAULT 0,
    status ebd_lesson_status NOT NULL DEFAULT 'DRAFT',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ebd_lesson_class ON ebd_lesson(class_id);
CREATE INDEX IF NOT EXISTS idx_ebd_lesson_order ON ebd_lesson(class_id, display_order);

CREATE TRIGGER trigger_ebd_lesson_updated_at
    BEFORE UPDATE ON ebd_lesson
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Material da turma (ementa) OU de uma aula específica — lesson_id nulo =
-- geral da turma, preenchido = específico da aula (mesmo princípio de
-- team_id nulo/preenchido em serving_area_member). file_data é o conteúdo
-- binário em si; file_size_bytes fica desnormalizado para listagem/exibição
-- sem precisar carregar o BYTEA inteiro (ver EbdMaterialRepository: as
-- consultas de listagem nunca selecionam file_data, só o download).
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
