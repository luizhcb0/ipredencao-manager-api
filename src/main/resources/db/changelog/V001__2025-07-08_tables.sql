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
CREATE TYPE tipo_relacionamento AS ENUM ('CONJUGE', 'NOIVO', 'NAMORADO', 'FILHO', 'PAI', 'MAE', 'IRMÃO', 'RESPONSAVEL');
CREATE TYPE regiao AS ENUM (
    'ÁGUAS_CLARAS', 'ARNIQUEIRA', 'BRAZLÂNDIA', 'CANDANGOLÂNDIA', 'CEILÂNDIA',
    'CRUZEIRO', 'ESTRUTURAL_SCIA', 'FERCAL', 'GAMA', 'GUARÁ', 'ITAPOÃ',
    'JARDIM_BOTÂNICO', 'LAGO_NORTE', 'LAGO_SUL', 'NÚCLEO_BANDEIRANTE', 'PARANOÁ',
    'PARK_WAY', 'PLANALTINA', 'PLANO_PILOTO', 'RECANTO_DAS_EMAS', 'RIACHO_FUNDO_I',
    'RIACHO_FUNDO_II', 'SIA', 'SAMAMBAIA', 'SANTA_MARIA', 'SOBRADINHO',
    'SOBRADINHO_II', 'SOL_NASCENTE_PÔR_DO_SOL', 'SUDOESTE_OCTOGONAL', 'SÃO_SEBASTIÃO',
    'TAGUATINGA', 'VARJÃO', 'VICENTE_PIRES'
);
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
('Transferido'),
('Excluido'),
('Missionário'),
('Possível admissão: gestação'),
('Pessoa referenciada');

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
('Membro não comungante, aguardando profissão de fé (admitindo)', 3),
('Membro não comungante, aguardando exame para profissão de fé (admitindo)', 3),
('Membro não comungante, em catequização final', 3),
('Membro não comungante (especial), não requer profissão de fé', 3),
('Membro não comungante', 3),
('Membro não comungante, em idade para profissão de fé', 3),

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

-- Categorias do agregador Transferido
('Transferido para outra igreja', 6),

-- Categorias do agregador Excluido
('Excluído a pedido', 7),
('Excluído por abandono', 7),
('Excluído por disciplina', 7),
('Excluído por falecimento', 7),

-- Categorias do agregador Missionário
('Missionário apoiado', 8),
('Missionário eventualmente auxiliado', 8),

-- Categorias do agregador Possível admissão: gestação
('Gestação', 9),
('Gestação mantida em sigilo temporariamente', 9),

-- Categorias do agregador Pessoa referenciada
('Agregado ou familiar', 10);


CREATE TABLE IF NOT EXISTS pessoa (
    pessoa_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    sexo sexo,
    apelido VARCHAR(255),
    email VARCHAR(255),
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30),
    telefones_secundarios VARCHAR(255)[],
    endereco_cep VARCHAR(12),
    endereco_logradouro VARCHAR(255),
    endereco_numero VARCHAR(20),
    endereco_complemento VARCHAR(255),
    campus VARCHAR(255),
    data_nascimento TIMESTAMP,
    cpf VARCHAR(20),
    rg VARCHAR(20),
    estado_civil estado_civil,
    igreja_anterior VARCHAR(255),
    situacao_igreja_anterior VARCHAR(255),
--  Se for cadastramento de pedido de membresia.
    tempo_na_igreja VARCHAR(100),
    motivos_para_admissao VARCHAR(255),
--
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100),
    empresa VARCHAR(100),
    regiao regiao,
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
    endereco_cep VARCHAR(12),
    endereco_logradouro VARCHAR(255),
    endereco_numero VARCHAR(20),
    endereco_complemento VARCHAR(255),
    campus VARCHAR(255),
    data_nascimento TIMESTAMP,
    cpf VARCHAR(20),
    rg VARCHAR(20),
    estado_civil estado_civil,
    igreja_anterior VARCHAR(255),
    situacao_igreja_anterior VARCHAR(255),
--  Se for cadastramento de pedido de membresia.
    tempo_na_igreja VARCHAR(100),
    motivos_para_admissao VARCHAR(255),
--
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100),
    empresa VARCHAR(100),
    regiao regiao,
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
    email VARCHAR(255) NOT NULL UNIQUE,
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30) NOT NULL,
    telefones_secundarios VARCHAR(255)[],
    endereco_cep VARCHAR(12) NOT NULL,
    endereco_logradouro VARCHAR(255) NOT NULL,
    endereco_numero VARCHAR(20) NOT NULL,
    endereco_complemento VARCHAR(255),
    campus VARCHAR(255) NOT NULL,
    data_nascimento TIMESTAMP NOT NULL,
    cpf VARCHAR(20) NOT NULL,
    rg VARCHAR(20) NOT NULL,
    estado_civil estado_civil,
    nome_pessoa_relacionada VARCHAR(255),
    inicio_relacionamento TIMESTAMP,
    nome_pai VARCHAR(255),
    nome_mae VARCHAR(255),
    nome_filhos VARCHAR(255)[],
    igreja_anterior VARCHAR(255),
    situacao_igreja_anterior VARCHAR(255),
    tempo_na_igreja VARCHAR(100),
    motivos_para_admissao VARCHAR(255),
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    data_profissao_de_fe TIMESTAMP,
    igreja_batismo VARCHAR(255),
    profissao VARCHAR(100),
    empresa VARCHAR(100),
    regiao regiao,
    foto_url VARCHAR(500),
    chefe_de_familia VARCHAR(255),
    propagar_endereco_chefe_familia BOOLEAN,
    pessoa_id BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES categoria(id),
    status form_pessoa_status NOT NULL DEFAULT 'CADASTRADO',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
