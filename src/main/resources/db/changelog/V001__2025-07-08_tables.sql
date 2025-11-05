-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Tipos personalizados
CREATE TYPE sexo AS ENUM ('MASCULINO', 'FEMININO');
CREATE TYPE estado_civil AS ENUM (
    'CASADO',
    'SOLTEIRO_SEM_RELACIONAMENTO',
    'SOLTEIRO_NAMORANDO',
    'SOLTEIRO_NOIVO',
    'DIVORCIADO_SEM_RELACIONAMENTO',
    'DIVORCIADO_NAMORANDO',
    'DIVORCIADO_NOIVO',
    'VIUVO_SEM_RELACIONAMENTO',
    'VIUVO_NAMORANDO',
    'VIUVO_NOIVO'
);
CREATE TYPE igreja_campus AS ENUM ('SEDE', 'CONGREGACAO');
CREATE TYPE tipo_batismo AS ENUM ('INFANTIL', 'ADULTO', 'NAO_BATIZADO');
CREATE TYPE tipo_relacionamento AS ENUM ('CONJUGE', 'NOIVO', 'NAMORADO', 'FILHO', 'PAI', 'MAE', 'IRMÃO', 'RESPONSAVEL', 'VIUVO');
CREATE TYPE form_pessoa_status AS ENUM ('CADASTRADO', 'VALIDADO');

-- Tabela de agregadores de categorias
CREATE TABLE IF NOT EXISTS agregador_categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

-- Inserir agregadores de categorias
INSERT INTO agregador_categoria (nome) VALUES
('Pastor'),
('Membro comungante'),
('Membro não comungante'),
('Rol à parte'),
('Admitendo'),
('Missionário'),
('Possível admissão: gestação'),
('Agregado não membro'),
('Pessoa referenciada'),
('Ex-membro da igreja');

-- Tabela de categorias
CREATE TABLE IF NOT EXISTS categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    agregador_categoria_id BIGINT NOT NULL REFERENCES agregador_categoria(id) ON DELETE CASCADE
);

-- Inserir categorias
INSERT INTO categoria (nome, agregador_categoria_id) VALUES
-- Categorias do agregador Pastor
('Pastor da igreja', 1),
('Pastor congregante', 1),

-- Categorias do agregador Membro comungante
('Membro comungante', 2),

-- Categorias do agregador Membro não comungante
('Aguardando profissão de fé', 3),
('Aguardando exame para profissão de fé', 3),
('Eespecial, não requer profissão de fé', 3),
('Membro não comungante', 3),
('Em idade para profissão de fé', 3),

-- Categorias do agregador Rol à parte
('Membro em trânsito', 4),
('Membro ausente', 4),
('Membro a transferir', 4),
('Membro não localizado ou pedido de desligamento', 4),
('Membro sob disciplina', 4),

-- Categorias do agregador Admitendo
('Aguardando carta de transferência', 5),
('Solicitar carta de transferência', 5),
('Aguardando batismo infantil', 5),
('Aguardando votos de membresia', 5),
('Aguardando profissão de fé', 5),
('Aguardando profissão de fé e batismo', 5),
('Aguardando exame para profissão de fé', 5),
('Aguardando casamento com membro da Igreja', 5),
('Aguardando resolução pendência', 5),
('Aguardando entrevista', 5),
('Em catequização', 5),
('Admissão sobrestada (pedido, impedim. ou discord. CFW)', 5),
('Admissão: batismo de menor sobrestado (credobatismo)', 5),

-- Categorias do agregador Missionário
('Missionário apoiado', 6),
('Missionário eventualmente auxiliado', 6),

-- Categorias do agregador Possível admissão: gestação
('Gestação', 7),
('Gestação mantida em sigilo temporariamente', 7),

-- Categorias do agregador Agregado não membro
('Agregado ou familiar', 8),

-- Categorias do agregador Pessoa referenciada
('Pessoa referenciada', 9),

-- Categorias do agregador Ex-membro da igreja
('Ex-membro', 10);

CREATE TABLE IF NOT EXISTS endereco (
    id BIGSERIAL PRIMARY KEY,
    cep VARCHAR(12),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(255),
    cidade VARCHAR(100),
    estado VARCHAR(32),
    coordenadas VARCHAR(64),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);


CREATE TABLE IF NOT EXISTS pessoa (
    pessoa_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    sexo sexo,
    apelido VARCHAR(255),
    email VARCHAR(255),
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30),
    telefones_secundarios VARCHAR(255)[],
    campus VARCHAR(255),
    data_nascimento TIMESTAMP,
    data_falecimento TIMESTAMP,
    cpf VARCHAR(60),
    rg VARCHAR(60),
    endereco_id BIGINT REFERENCES endereco(id),
    estado_civil estado_civil,
    igreja_anterior TEXT,
    motivos_para_admissao TEXT,
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100)[],
    empresa VARCHAR(100)[],
    informacoes_adicionais TEXT,
    foto_url VARCHAR(500),
    chefe_de_familia BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES categoria(id),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS pessoa_history (
    history_id BIGSERIAL PRIMARY KEY,
    pessoa_id BIGINT REFERENCES pessoa(pessoa_id),
    nome VARCHAR(255) NOT NULL,
    sexo sexo,
    apelido VARCHAR(255),
    email VARCHAR(255),
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30),
    telefones_secundarios VARCHAR(255)[],
    campus VARCHAR(255),
    data_nascimento TIMESTAMP,
    data_falecimento TIMESTAMP,
    cpf VARCHAR(60),
    rg VARCHAR(60),
    endereco_id BIGINT REFERENCES endereco(id),
    estado_civil estado_civil,
    igreja_anterior TEXT,
    motivos_para_admissao TEXT,
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100)[],
    empresa VARCHAR(100)[],
    informacoes_adicionais TEXT,
    foto_url VARCHAR(500),
    chefe_de_familia BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES categoria(id),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);

-- Tabela de relacionamento entre pessoas
CREATE TABLE IF NOT EXISTS pessoa_relacionamento (
    id BIGSERIAL PRIMARY KEY,
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    pessoa_relacionada_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    tipo_relacionamento tipo_relacionamento NOT NULL,
    inicio_relacionamento TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pessoa_relacionamento_history (
    history_id BIGSERIAL PRIMARY KEY,
    id BIGINT REFERENCES pessoa_relacionamento(id),
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    pessoa_relacionada_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    tipo_relacionamento tipo_relacionamento NOT NULL,
    inicio_relacionamento TIMESTAMP
);

-- muito parecido com pessoa, porem provisorio
CREATE TABLE IF NOT EXISTS formulario_pessoa (
    formulario_pessoa_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    sexo sexo NOT NULL,
    apelido VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30) NOT NULL,
    telefones_secundarios VARCHAR(255)[],
    endereco_cep VARCHAR(12) NOT NULL,
    endereco_logradouro VARCHAR(255) NOT NULL,
    endereco_numero VARCHAR(20) NOT NULL,
    endereco_complemento VARCHAR(255),
    campus VARCHAR(255) NOT NULL,
    data_nascimento TIMESTAMP NOT NULL,
    cpf VARCHAR(60) NOT NULL,
    rg VARCHAR(60)NOT NULL,
    estado_civil estado_civil,
    nome_pessoa_relacionada VARCHAR(255),
    inicio_relacionamento TIMESTAMP,
    nome_pai VARCHAR(255),
    nome_mae VARCHAR(255),
    nome_filhos VARCHAR(255)[],
    igreja_anterior TEXT,
    motivos_para_admissao TEXT,
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100)[],
    empresa VARCHAR(100)[],
    foto_url VARCHAR(500),
    chefe_de_familia VARCHAR(255),
    propagar_endereco_chefe_familia BOOLEAN,
    pessoa_id BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES categoria(id),
    status form_pessoa_status NOT NULL DEFAULT 'CADASTRADO',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

--  updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_formulario_pessoa_updated_at
    BEFORE UPDATE ON formulario_pessoa
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_pessoa_updated_at
    BEFORE UPDATE ON pessoa
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_endereco_updated_at
    BEFORE UPDATE ON endereco
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger para histórico de pessoa
CREATE OR REPLACE FUNCTION insert_pessoa_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO pessoa_history (
        pessoa_id,
        nome,
        sexo,
        apelido,
        email,
        emails_secundarios,
        telefone,
        telefones_secundarios,
        campus,
        data_nascimento,
        data_falecimento,
        cpf,
        rg,
        endereco_id,
        estado_civil,
        igreja_anterior,
        motivos_para_admissao,
        tipo_batismo,
        data_batismo,
        data_profissao_de_fe,
        igreja_batismo,
        profissao,
        empresa,
        informacoes_adicionais,
        foto_url,
        chefe_de_familia,
        categoria_id,
        updated_by
    ) VALUES (
        NEW.pessoa_id,
        NEW.nome,
        NEW.sexo,
        NEW.apelido,
        NEW.email,
        NEW.emails_secundarios,
        NEW.telefone,
        NEW.telefones_secundarios,
        NEW.campus,
        NEW.data_nascimento,
        NEW.data_falecimento,
        NEW.cpf,
        NEW.rg,
        NEW.endereco_id,
        NEW.estado_civil,
        NEW.igreja_anterior,
        NEW.motivos_para_admissao,
        NEW.tipo_batismo,
        NEW.data_batismo,
        NEW.data_profissao_de_fe,
        NEW.igreja_batismo,
        NEW.profissao,
        NEW.empresa,
        NEW.informacoes_adicionais,
        NEW.foto_url,
        NEW.chefe_de_familia,
        NEW.categoria_id,
        NEW.updated_by
    );
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_pessoa_history
    AFTER INSERT OR UPDATE ON pessoa
    FOR EACH ROW
    EXECUTE FUNCTION insert_pessoa_history();


-- Índices para tabela ENDERECO
CREATE INDEX IF NOT EXISTS idx_endereco_logradouro ON endereco(logradouro);
CREATE INDEX IF NOT EXISTS idx_endereco_cep ON endereco(cep);
CREATE INDEX IF NOT EXISTS idx_endereco_logradouro_cep ON endereco(logradouro, cep);

-- Índices para tabela PESSOA
CREATE INDEX IF NOT EXISTS idx_pessoa_nome ON pessoa(nome);
CREATE INDEX IF NOT EXISTS idx_pessoa_email ON pessoa(email);
CREATE INDEX IF NOT EXISTS idx_pessoa_cpf ON pessoa(cpf);
CREATE INDEX IF NOT EXISTS idx_pessoa_telefone ON pessoa(telefone);
CREATE INDEX IF NOT EXISTS idx_pessoa_endereco_id ON pessoa(endereco_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_categoria_id ON pessoa(categoria_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_campus ON pessoa(campus);
CREATE INDEX IF NOT EXISTS idx_pessoa_categoria_id_added_at ON pessoa(categoria_id,added_at);
CREATE INDEX IF NOT EXISTS idx_pessoa_added_at ON pessoa(added_at);

-- Índices para tabela PESSOA_RELACIONAMENTO
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_pessoa_id ON pessoa_relacionamento(pessoa_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_pessoa_relacionada_id ON pessoa_relacionamento(pessoa_relacionada_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_tipo ON pessoa_relacionamento(tipo_relacionamento);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_composto ON pessoa_relacionamento(pessoa_id, pessoa_relacionada_id, tipo_relacionamento);

-- Índices para tabela PESSOA_HISTORY
CREATE INDEX IF NOT EXISTS idx_pessoa_history_pessoa_id ON pessoa_history(pessoa_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_history_added_at ON pessoa_history(added_at DESC);
