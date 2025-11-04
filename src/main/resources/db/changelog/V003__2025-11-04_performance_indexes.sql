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

-- Índices para tabela PESSOA_RELACIONAMENTO
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_pessoa_id ON pessoa_relacionamento(pessoa_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_pessoa_relacionada_id ON pessoa_relacionamento(pessoa_relacionada_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_tipo ON pessoa_relacionamento(tipo_relacionamento);
CREATE INDEX IF NOT EXISTS idx_pessoa_relacionamento_composto ON pessoa_relacionamento(pessoa_id, pessoa_relacionada_id, tipo_relacionamento);

-- Índices para tabela PESSOA_HISTORY
CREATE INDEX IF NOT EXISTS idx_pessoa_history_pessoa_id ON pessoa_history(pessoa_id);
CREATE INDEX IF NOT EXISTS idx_pessoa_history_added_at ON pessoa_history(added_at DESC);
