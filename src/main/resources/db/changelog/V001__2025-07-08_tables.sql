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

-- Tabela de categorias principais
CREATE TABLE IF NOT EXISTS categoria (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL
);

-- Inserir categorias principais
INSERT INTO categoria (codigo, nome) VALUES
('00', 'Pastores'),
('01', 'Membros comungantes'),
('02', 'Membros comungantes (rol à parte)'),
('03', 'Membros não comungantes'),
('04', 'Admissão'),
('05', 'Em processo de admissão'),
('06', 'Em avaliação para admissão'),
('07', 'Pastor congregante'),
('08', 'Agregado não membro (p.ex. familiar frequente)'),
('09', 'Pessoa fictícia (sistema)'),
('40', 'Visitantes frequentes'),
('48', 'Aparece na contabilidade'),
('49', 'Possível admissão: gestação'),
('50', 'Missionários'),
('51', 'Missionários apoiados'),
('52', 'Missionários eventualmente auxiliados'),
('80', 'Oficiais da IPB'),
('90', 'Visitantes'),
('91', 'Ex-membro da Igreja'),
('95', 'Visitante'),
('97', 'Relacionamento profissional'),
('98', 'Organização'),
('99', 'Pessoa referenciada (sistema)');

-- Tabela de subcategorias
CREATE TABLE IF NOT EXISTS subcategoria (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    categoria_id BIGINT NOT NULL REFERENCES categoria(id) ON DELETE CASCADE
);

-- Inserir subcategorias
INSERT INTO subcategoria (codigo, nome, categoria_id) VALUES
-- Categoria 00 (Pastores)
('00', 'Pastores da Igreja', (SELECT id FROM categoria WHERE codigo = '00')),

-- Categoria 01 (Membros comungantes)
('01', 'Membro', (SELECT id FROM categoria WHERE codigo = '01')),

-- Subcategorias da categoria 02 (Membros comungantes - rol à parte)
('02.01', 'Rol à parte, membro em trânsito', (SELECT id FROM categoria WHERE codigo = '02')),
('02.03', 'Rol à parte, membro ausente (doença etc.)', (SELECT id FROM categoria WHERE codigo = '02')),
('02.90', 'Rol à parte, membro a transferir', (SELECT id FROM categoria WHERE codigo = '02')),
('02.95', 'Rol à parte, membro não localizado ou pedido de desligamento', (SELECT id FROM categoria WHERE codigo = '02')),
('02.99', 'Rol à parte, membro sob disciplina', (SELECT id FROM categoria WHERE codigo = '02')),

-- Subcategorias da categoria 03 (Membros não comungantes)
('03.00', 'Membro não comungante, aguardando profissão de fé (admitindo)', (SELECT id FROM categoria WHERE codigo = '03')),
('03.01', 'Membro não comungante, aguardando exame para profissão de fé (admitindo)', (SELECT id FROM categoria WHERE codigo = '03')),
('03.02', 'Membro não comungante, em catequização final', (SELECT id FROM categoria WHERE codigo = '03')),
('03.95', 'Membro não comungante (especial), não requer profissão de fé', (SELECT id FROM categoria WHERE codigo = '03')),
('03.96', 'Membro não comungante', (SELECT id FROM categoria WHERE codigo = '03')),
('03.97', 'Membro não comungante, em idade para profissão de fé', (SELECT id FROM categoria WHERE codigo = '03')),
('03.98', 'Membro não comungante, rol à parte (em trânsito)', (SELECT id FROM categoria WHERE codigo = '03')),
('03.99', 'Membro não comungante, rol à parte (transf)', (SELECT id FROM categoria WHERE codigo = '03')),

-- Subcategorias da categoria 04 (Admissão)
('04.00', 'Admissão, rotina de admissão', (SELECT id FROM categoria WHERE codigo = '04')),
('04.01', 'Admissão, aguardando registro em ata', (SELECT id FROM categoria WHERE codigo = '04')),
('04.98', 'Admissão, aguardando carta de transferência', (SELECT id FROM categoria WHERE codigo = '04')),
('04.99', 'Admissão, solicitar carta de transferência', (SELECT id FROM categoria WHERE codigo = '04')),

-- Subcategorias da categoria 05 (Em processo de admissão)
('05.01', 'Admitendo, menor batizado a admitir juntamente com pais ou responsáveis', (SELECT id FROM categoria WHERE codigo = '05')),
('05.10', 'Admitendo, menor aguardando batismo infantil', (SELECT id FROM categoria WHERE codigo = '05')),
('05.15', 'Admitendo, não presbiteriano aguardando votos de membresia', (SELECT id FROM categoria WHERE codigo = '05')),
('05.50', 'Admitendo, adulto/jovem aguardando profissão de fé', (SELECT id FROM categoria WHERE codigo = '05')),
('05.51', 'Admitendo, adulto/jovem aguardando profissão de fé e batismo', (SELECT id FROM categoria WHERE codigo = '05')),
('05.59', 'Admitendo, aguardando exame para profissão de fé', (SELECT id FROM categoria WHERE codigo = '05')),
('05.60', 'Admitendo, aguardando casamento com membro da Igreja', (SELECT id FROM categoria WHERE codigo = '05')),
('05.70', 'Admitendo, aguardando resolução pendência', (SELECT id FROM categoria WHERE codigo = '05')),
('05.90', 'Admitendo, aguardando entrevista', (SELECT id FROM categoria WHERE codigo = '05')),

-- Subcategorias da categoria 06 (Em avaliação para admissão)
('06.01', 'Possível admissão: em catequização', (SELECT id FROM categoria WHERE codigo = '06')),
('06.94', 'Possível admissão: filhos de pais em catequização', (SELECT id FROM categoria WHERE codigo = '06')),
('06.95', 'Possível admissão: gestação', (SELECT id FROM categoria WHERE codigo = '06')),
('06.96', 'Possível admissão: admissão sobrestada (pedido, impedim. ou discord. CFW)', (SELECT id FROM categoria WHERE codigo = '06')),
('06.97', 'Possível admissão: batismo de menor sobrestado (credobatismo)', (SELECT id FROM categoria WHERE codigo = '06')),
('06.98', 'Possível admissão: em avaliação', (SELECT id FROM categoria WHERE codigo = '06')),
('06.99', 'Possível admissão: aguardando ficha cadastral', (SELECT id FROM categoria WHERE codigo = '06')),

-- Categoria 07 (Pastor congregante)
('07', 'Pastor congregante', (SELECT id FROM categoria WHERE codigo = '07')),

-- Categoria 08 (Agregado não membro)
('08', 'Agregado não membro (p.ex. familiar frequente)', (SELECT id FROM categoria WHERE codigo = '08')),

-- Categoria 09 (Pessoa fictícia)
('09', 'Pessoa fictícia (sistema)', (SELECT id FROM categoria WHERE codigo = '09')),

-- Subcategorias da categoria 40 (Visitantes frequentes)
('42', 'Visitante recorrente EBD/GF/3aIdade/EnglishBibleStudy/Youtube', (SELECT id FROM categoria WHERE codigo = '40')),
('43', 'Visitante frequente', (SELECT id FROM categoria WHERE codigo = '40')),
('44', 'Visitante frequente (menor de idade)', (SELECT id FROM categoria WHERE codigo = '40')),
('45', 'Visitante frequente, mas sem intenção de admissão', (SELECT id FROM categoria WHERE codigo = '40')),
('46', 'Visitante evento (AcampUMP, Conferência etc.)', (SELECT id FROM categoria WHERE codigo = '40')),

-- Categoria 48 (Aparece na contabilidade)
('48', 'Aparece na contabilidade', (SELECT id FROM categoria WHERE codigo = '48')),

-- Categoria 49 (Possível admissão: gestação)
('49', 'Possível admissão: gestação (mantida em sigilo, temporariamente)', (SELECT id FROM categoria WHERE codigo = '49')),

-- Categoria 51 (Missionários apoiados)
('51', 'Missionários apoiados', (SELECT id FROM categoria WHERE codigo = '51')),

-- Categoria 52 (Missionários eventualmente auxiliados)
('52', 'Missionários eventualmente auxiliados', (SELECT id FROM categoria WHERE codigo = '52')),

-- Categoria 80 (Oficiais da IPB)
('80', 'Oficiais da IPB', (SELECT id FROM categoria WHERE codigo = '80')),

-- Categoria 90 (Visitantes)
('90', 'Visitante ocasional periódico', (SELECT id FROM categoria WHERE codigo = '90')),

-- Categoria 91 (Ex-membro da Igreja)
('91', 'Ex-membro da Igreja', (SELECT id FROM categoria WHERE codigo = '91')),

-- Categoria 95 (Visitante)
('95', 'Visitante', (SELECT id FROM categoria WHERE codigo = '95')),

-- Categoria 97 (Relacionamento profissional)
('97', 'Relacionamento profissional', (SELECT id FROM categoria WHERE codigo = '97')),

-- Categoria 98 (Organização)
('98', 'Organização', (SELECT id FROM categoria WHERE codigo = '98')),

-- Categoria 99 (Pessoa referenciada)
('99', 'Pessoa referenciada (sistema)', (SELECT id FROM categoria WHERE codigo = '99'));


CREATE TABLE IF NOT EXISTS pessoa (
    pessoa_id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    sexo sexo,
    apelido VARCHAR(255),
    email VARCHAR(255),
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30),
    telefones_secundarios VARCHAR(255)[],
    endereco VARCHAR(255),
    cep VARCHAR(12),
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
--  Pode ser extraído de link do google.
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    foto_url VARCHAR(500),
    chefe_de_familia BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES subcategoria(id),
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
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
    endereco VARCHAR(255),
    cep VARCHAR(12),
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
--  Pode ser extraído de link do google.
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    foto_url VARCHAR(500),
    chefe_de_familia BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES subcategoria(id),
    added_at TIMESTAMP NOT NULL DEFAULT NOW()
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
    apelido VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30) NOT NULL,
    telefones_secundarios VARCHAR(255)[],
    endereco VARCHAR(255),
    cep VARCHAR(12),
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
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    foto_url VARCHAR(500),
    chefe_de_familia VARCHAR(255),
    propagar_endereco_chefe_familia BOOLEAN,
    pessoa_id BIGINT REFERENCES pessoa(pessoa_id),
    categoria_id BIGINT REFERENCES subcategoria(id),
    status form_pessoa_status NOT NULL DEFAULT 'CADASTRADO',
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
