-- Áreas de serviço (serving areas): ministérios e sociedades internas (Música,
-- GF, Mocidade, Mesa de som, ...) e os órgãos de mandato (Conselho, Junta
-- Diaconal e os serviços de disponibilidade). Cada área define seu próprio
-- catálogo de cargos, pode ter equipes opcionais e agrega os vínculos
-- (mandatos) das pessoas. Nome interno "serving_area" para não colidir com a
-- camada de serviços (service/) da aplicação; na UI é "Serviço".

-- Tipo de cargo: nível hierárquico do vínculo dentro da área.
-- SUPERVISION  = supervisão (no máx. 1 por área).
-- COORDINATION = liderança (coordenação geral ou liderança de equipe).
-- MEMBERSHIP   = membro/participante.
CREATE TYPE serving_area_position_kind AS ENUM ('SUPERVISION', 'COORDINATION', 'MEMBERSHIP');

CREATE TABLE IF NOT EXISTS serving_area (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    -- Grupo de WhatsApp geral da área (todos os membros); opcional.
    whatsapp_url VARCHAR(512),
    -- Grupo de WhatsApp da coordenação geral (coordenadores/diretoria); opcional
    -- e independente de equipes.
    coordination_whatsapp_url VARCHAR(512),
    -- Desativado = oculto de novos vínculos, preservando histórico (exceção
    -- documentada ao "hard deletes only": ver SPEC.md).
    active BOOLEAN NOT NULL DEFAULT TRUE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

-- Catálogo de cargos por área. O escopo (geral x equipe) vem do vínculo
-- (team_id), não do nome do cargo.
CREATE TABLE IF NOT EXISTS serving_area_position (
    id BIGSERIAL PRIMARY KEY,
    serving_area_id BIGINT NOT NULL REFERENCES serving_area(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    kind serving_area_position_kind NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT,
    UNIQUE (serving_area_id, name)
);

-- Equipes opcionais de uma área (grupos de violões, GF por localidade, ...).
CREATE TABLE IF NOT EXISTS serving_area_team (
    id BIGSERIAL PRIMARY KEY,
    serving_area_id BIGINT NOT NULL REFERENCES serving_area(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    -- Grupo de WhatsApp da equipe; opcional.
    whatsapp_url VARCHAR(512),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT,
    UNIQUE (serving_area_id, name)
);

-- Vínculo (mandato) de uma pessoa em uma área, com cargo e equipe opcional.
-- Encerramento = DELETE (histórico em serving_area_member_history).
-- position_id/team_id usam NO ACTION
-- (não RESTRICT/SET NULL): a checagem no fim do statement deixa o ON DELETE
-- CASCADE da área apagar membros + cargos + equipes numa tacada; a proteção de
-- "cargo/equipe em uso" fica no service layer.
CREATE TABLE IF NOT EXISTS serving_area_member (
    id BIGSERIAL PRIMARY KEY,
    serving_area_id BIGINT NOT NULL REFERENCES serving_area(id) ON DELETE CASCADE,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    position_id BIGINT NOT NULL REFERENCES serving_area_position(id) ON DELETE NO ACTION,
    team_id BIGINT REFERENCES serving_area_team(id) ON DELETE NO ACTION,
    start_date DATE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT
);

CREATE TRIGGER trigger_serving_area_updated_at
    BEFORE UPDATE ON serving_area
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_serving_area_position_updated_at
    BEFORE UPDATE ON serving_area_position
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_serving_area_team_updated_at
    BEFORE UPDATE ON serving_area_team
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_serving_area_member_updated_at
    BEFORE UPDATE ON serving_area_member
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Anti-duplicata (reforço): a validação principal fica no service layer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_serving_area_member
    ON serving_area_member (serving_area_id, person_id, position_id, COALESCE(team_id, 0));

CREATE INDEX IF NOT EXISTS idx_serving_area_position_area ON serving_area_position(serving_area_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_position_kind ON serving_area_position(kind);
CREATE INDEX IF NOT EXISTS idx_serving_area_team_area     ON serving_area_team(serving_area_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_area   ON serving_area_member(serving_area_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_person ON serving_area_member(person_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_pos    ON serving_area_member(position_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_team   ON serving_area_member(team_id);

-- Histórico de vínculos: espelha serving_area_member; deleted_at preenchido na
-- remoção. Triggers mantêm o histórico em INSERT/UPDATE/DELETE (como pessoa_history).
CREATE TABLE IF NOT EXISTS serving_area_member_history (
    history_id BIGSERIAL PRIMARY KEY,
    member_id BIGINT REFERENCES serving_area_member(id) ON DELETE SET NULL,
    serving_area_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    team_id BIGINT,
    start_date DATE,
    added_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT REFERENCES usuario(id) ON DELETE RESTRICT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_serving_area_member_history_member
    ON serving_area_member_history(member_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_history_area
    ON serving_area_member_history(serving_area_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_history_person
    ON serving_area_member_history(person_id);
CREATE INDEX IF NOT EXISTS idx_serving_area_member_history_deleted_at
    ON serving_area_member_history(deleted_at DESC);

CREATE OR REPLACE FUNCTION insert_serving_area_member_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO serving_area_member_history (
        member_id,
        serving_area_id,
        person_id,
        position_id,
        team_id,
        start_date,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        NEW.id,
        NEW.serving_area_id,
        NEW.person_id,
        NEW.position_id,
        NEW.team_id,
        NEW.start_date,
        NEW.added_at,
        NEW.updated_at,
        NEW.updated_by,
        NULL
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_serving_area_member_history
    AFTER INSERT OR UPDATE ON serving_area_member
    FOR EACH ROW
    EXECUTE FUNCTION insert_serving_area_member_history();

CREATE OR REPLACE FUNCTION delete_serving_area_member_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO serving_area_member_history (
        member_id,
        serving_area_id,
        person_id,
        position_id,
        team_id,
        start_date,
        added_at,
        updated_at,
        updated_by,
        deleted_at
    ) VALUES (
        OLD.id,
        OLD.serving_area_id,
        OLD.person_id,
        OLD.position_id,
        OLD.team_id,
        OLD.start_date,
        OLD.added_at,
        OLD.updated_at,
        OLD.updated_by,
        CURRENT_TIMESTAMP
    );
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_serving_area_member_history_delete
    BEFORE DELETE ON serving_area_member
    FOR EACH ROW
    EXECUTE FUNCTION delete_serving_area_member_history();

-- Seeds: quatro áreas fixas (mandato/disponibilidade) criadas como áreas
-- comuns, cada uma com um único cargo MEMBERSHIP e sem membros. Não têm
-- tratamento especial; recomendação operacional é não renomeá-las. Áreas
-- criadas pela aplicação recebem os 3 cargos padrão (Supervisor/Coordenador/
-- Membro) no service layer.
INSERT INTO serving_area (name, description) VALUES
('Conselho',                       'Conselho de presbíteros da igreja'),
('Junta Diaconal',                 'Junta diaconal da igreja'),
('Presbítero em disponibilidade',  'Presbíteros em disponibilidade'),
('Diácono em disponibilidade',     'Diáconos em disponibilidade');

INSERT INTO serving_area_position (serving_area_id, name, kind)
SELECT id, 'Presbítero', 'MEMBERSHIP' FROM serving_area WHERE name = 'Conselho';

INSERT INTO serving_area_position (serving_area_id, name, kind)
SELECT id, 'Diácono', 'MEMBERSHIP' FROM serving_area WHERE name = 'Junta Diaconal';

INSERT INTO serving_area_position (serving_area_id, name, kind)
SELECT id, 'Presbítero em disponibilidade', 'MEMBERSHIP' FROM serving_area WHERE name = 'Presbítero em disponibilidade';

INSERT INTO serving_area_position (serving_area_id, name, kind)
SELECT id, 'Diácono em disponibilidade', 'MEMBERSHIP' FROM serving_area WHERE name = 'Diácono em disponibilidade';
