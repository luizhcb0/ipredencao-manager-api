-- Arquivo seguindo padrão existente V001__2025-07-08_tables.sql

-- ENUMs para autenticação (seguindo padrão existente)
CREATE TYPE perfil_acesso AS ENUM ('BOLETIM', 'PRESBITERO', 'ADMIN');
CREATE TYPE provider_autenticacao AS ENUM ('GOOGLE', 'FACEBOOK', 'APPLE', 'EMAIL');

-- Tabelas de autenticação
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    access_profile perfil_acesso NOT NULL DEFAULT 'BOLETIM',
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    provider provider_autenticacao NOT NULL,
    failed_login_attempts INTEGER DEFAULT 0,
    blocked_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessoes_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) UNIQUE NOT NULL,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP NOT NULL,
    data_ultimo_uso TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_agent TEXT,
    dispositivo VARCHAR(100),
    localizacao VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_usuarios_firebase_uid ON usuarios(firebase_uid);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email);
CREATE INDEX IF NOT EXISTS idx_usuarios_perfil_ativo ON usuarios(access_profile, active);
CREATE INDEX IF NOT EXISTS idx_sessoes_usuario_id ON sessoes_usuario(usuario_id);
CREATE INDEX IF NOT EXISTS idx_sessoes_ativo_expiracao ON sessoes_usuario(ativo, data_expiracao);

-- Trigger para updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
