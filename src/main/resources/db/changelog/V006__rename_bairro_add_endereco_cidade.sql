ALTER TABLE formulario_pessoa RENAME COLUMN bairro TO endereco_bairro;
ALTER TABLE formulario_pessoa ADD COLUMN IF NOT EXISTS endereco_cidade VARCHAR(100);
