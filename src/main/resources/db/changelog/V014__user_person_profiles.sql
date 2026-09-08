-- Novos perfis sem acesso; vínculo opcional 1:1 usuario → pessoa.

ALTER TYPE perfil_acesso ADD VALUE IF NOT EXISTS 'MEMBERSHIP_CANDIDATE';
ALTER TYPE perfil_acesso ADD VALUE IF NOT EXISTS 'MEMBER';

ALTER TABLE usuario
    ADD COLUMN person_id BIGINT UNIQUE REFERENCES pessoa(pessoa_id) ON DELETE SET NULL;
