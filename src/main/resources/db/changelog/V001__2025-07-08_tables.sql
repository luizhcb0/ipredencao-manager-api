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
CREATE TYPE status AS ENUM ('CADASTRADO', 'VALIDADO');

CREATE TABLE IF NOT EXISTS pessoa (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    apelido VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    emails_secundarios VARCHAR(255)[],
    telefone VARCHAR(30) NOT NULL,
    telefones_secundarios VARCHAR(255)[],
    campus VARCHAR(255) NOT NULL,
    data_nascimento TIMESTAMP NOT NULL,
    cpf VARCHAR(20) NOT NULL,
    rg VARCHAR(20) NOT NULL,
    estado_civil estado_civil NOT NULL,
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
    endereco VARCHAR(255),
    regiao regiao,
--  Pode ser extraído de link do google.
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    foto_url VARCHAR(500),
    status status NOT NULL DEFAULT 'CADASTRADO'
);

-- Tabela de relacionamento entre pessoas
CREATE TABLE IF NOT EXISTS pessoa_relacionamento (
    id BIGSERIAL PRIMARY KEY,
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
    pessoa_relacionada_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
    tipo_relacionamento tipo_relacionamento NOT NULL,
    inicio_relacionamento TIMESTAMP
); 