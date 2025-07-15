CREATE TYPE estado_civil AS ENUM ('SOLTEIRO', 'CASADO', 'DIVORCIADO', 'VIUVO', 'SEPARADO');
CREATE TYPE tipo_batismo AS ENUM ('INFANTIL', 'ADULTO', 'NAO_BATIZADO');
CREATE TYPE estado_pessoa AS ENUM ('ATIVO', 'INATIVO', 'FALECIDO', 'TRANSFERIDO');
CREATE TYPE tipo_admissao AS ENUM ('BATISMO', 'PROFISSAO_DE_FE', 'TRANSFERENCIA', 'OUTROS');
CREATE TYPE tipo_relacionamento AS ENUM ('CONJUGE', 'FILHO', 'PAI', 'MAE', 'IRMÃO', 'RESPONSAVEL');

CREATE TABLE IF NOT EXISTS pessoa (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    sede_congregacao VARCHAR(255),
    apelido VARCHAR(255),
    data_nascimento TIMESTAMP,
    telefone VARCHAR(30),
    estado_civil estado_civil,
    igreja_anterior VARCHAR(255),
    situacao_igreja_anterior VARCHAR(255),
    tempo_na_ipr VARCHAR(100),
    motivos_admissao VARCHAR(255),
    tipo_batismo tipo_batismo,
    data_batismo TIMESTAMP,
    igreja_batismo VARCHAR(255),
    dados_oficial VARCHAR(255),
    supervisoes VARCHAR(255),
    outras_categorias VARCHAR(255),
    estado_pessoa estado_pessoa,
    tipo_admissao tipo_admissao,
    cpf VARCHAR(20),
    rg VARCHAR(20),
    email_adicional VARCHAR(255),
    telefone_adicional VARCHAR(30),
    email_trabalho VARCHAR(255),
    telefone_trabalho VARCHAR(30),
    profissao VARCHAR(100),
    empresa VARCHAR(100),
    endereco VARCHAR(255),
    latitude NUMERIC(10,8),
    longitude NUMERIC(11,8),
    regiao_gf VARCHAR(100),
    skype VARCHAR(100),
    twitter VARCHAR(100),
    linkedin VARCHAR(100),
    instagram VARCHAR(100),
    facebook VARCHAR(100),
    pagina_pessoal VARCHAR(255),
    foto_url VARCHAR(500)
);

-- Tabela de relacionamento qualificado entre pessoas
CREATE TABLE IF NOT EXISTS pessoa_relacionamento (
    id BIGSERIAL PRIMARY KEY,
    pessoa_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
    pessoa_relacionada_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
    tipo_relacionamento tipo_relacionamento NOT NULL,
    inicio_relacionamento TIMESTAMP
); 