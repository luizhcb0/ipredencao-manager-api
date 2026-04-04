-- Remove a coluna informacoes_adicionais (dados migrados para person_note via backfill)

ALTER TABLE pessoa DROP COLUMN IF EXISTS informacoes_adicionais;
ALTER TABLE pessoa_history DROP COLUMN IF EXISTS informacoes_adicionais;

-- Recriar trigger sem a coluna removida
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
        NEW.foto_url,
        NEW.chefe_de_familia,
        NEW.categoria_id,
        NEW.updated_by
    );
    RETURN NEW;
END;
$$ language 'plpgsql';
