-- Tipos canonicos do Cap. III da CI/IPB (arts. 16, 17, 23, 24).
CREATE TABLE IF NOT EXISTS official_act_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(64) NOT NULL,
    reference_article VARCHAR(64) NOT NULL
);

INSERT INTO official_act_type (name, category, reference_article) VALUES
('Admissão de membro comungante',     'ADMISSAO', 'Art. 16'),
('Admissão de membro não comungante', 'ADMISSAO', 'Art. 17'),
('Demissão de membro comungante',     'DEMISSAO', 'Art. 23'),
('Demissão de membro não comungante', 'DEMISSAO', 'Art. 24');

-- Forma (alinea) do ato; padrao agregador_categoria -> categoria.
CREATE TABLE IF NOT EXISTS official_act_form (
    id BIGSERIAL PRIMARY KEY,
    official_act_type_id BIGINT NOT NULL REFERENCES official_act_type(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    article_clause VARCHAR(64) NOT NULL,
    UNIQUE (official_act_type_id, name)
);

INSERT INTO official_act_form (official_act_type_id, name, article_clause) VALUES
-- Admissao de membro comungante (Art. 16)
(1, 'Profissão de fé',                'Art. 16, a'),
(1, 'Profissão de fé e batismo',      'Art. 16, b'),
(1, 'Carta de transferência',         'Art. 16, c'),
(1, 'Jurisdição a pedido',            'Art. 16, d'),
(1, 'Jurisdição ex officio',          'Art. 16, e'),
(1, 'Restauração',                    'Art. 16, f'),
(1, 'Designação do Presbitério',      'Art. 16, g'),
-- Admissao de membro nao comungante (Art. 17)
(2, 'Batismo na infância',                               'Art. 17, a'),
(2, 'Transferência dos pais ou responsáveis',            'Art. 17, b'),
(2, 'Jurisdição assumida sobre os pais ou responsáveis', 'Art. 17, c'),
-- Demissao de membro comungante (Art. 23)
(3, 'Exclusão por disciplina',              'Art. 23, a'),
(3, 'Exclusão a pedido',                    'Art. 23, b'),
(3, 'Exclusão por ausência',                'Art. 23, c'),
(3, 'Carta de transferência',               'Art. 23, d'),
(3, 'Jurisdição assumida por outra igreja', 'Art. 23, e'),
(3, 'Falecimento',                          'Art. 23, f'),
(3, 'Ordenação ao ministério',              'Art. 23, §3'),
-- Demissao de membro nao comungante (Art. 24)
(4, 'Carta de transferência dos pais ou responsáveis',    'Art. 24, a'),
(4, 'Carta de transferência (do próprio menor)',          'Art. 24, b'),
(4, 'Atingimento da maioridade (18 anos)',                'Art. 24, c'),
(4, 'Profissão de fé',                                    'Art. 24, d'),
(4, 'Solicitação dos pais',                               'Art. 24, e'),
(4, 'Falecimento',                                        'Art. 24, f');

-- Sequencia global; atribuida pelo service (so para admissoes).
CREATE SEQUENCE IF NOT EXISTS official_act_admission_order_seq START 1;

CREATE TABLE IF NOT EXISTS official_act (
    id BIGSERIAL PRIMARY KEY,
    official_act_form_id BIGINT NOT NULL REFERENCES official_act_form(id),
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id),
    act_date DATE NOT NULL,
    minute_number VARCHAR(64),
    minute_date DATE,
    admission_order_number BIGINT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    notes TEXT,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);

CREATE TRIGGER trigger_official_act_updated_at
    BEFORE UPDATE ON official_act
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX IF NOT EXISTS idx_official_act_form_type     ON official_act_form(official_act_type_id);
CREATE INDEX IF NOT EXISTS idx_official_act_act_date      ON official_act(act_date DESC);
CREATE INDEX IF NOT EXISTS idx_official_act_form          ON official_act(official_act_form_id);
CREATE INDEX IF NOT EXISTS idx_official_act_minute_number ON official_act(minute_number);
CREATE INDEX IF NOT EXISTS idx_official_act_person        ON official_act(person_id);
CREATE INDEX IF NOT EXISTS idx_official_act_admission_order ON official_act(admission_order_number)
    WHERE admission_order_number IS NOT NULL;
