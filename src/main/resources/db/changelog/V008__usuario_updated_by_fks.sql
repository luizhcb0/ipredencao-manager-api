-- FKs em updated_by → usuario(id) impedem exclusão de usuário com registros vinculados.
-- Limpa referências órfãs antes de adicionar as constraints.

UPDATE endereco SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = endereco.updated_by);

UPDATE pessoa SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = pessoa.updated_by);

UPDATE pessoa_history SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = pessoa_history.updated_by);

UPDATE person_note SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = person_note.updated_by);

UPDATE person_note_history SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = person_note_history.updated_by);

UPDATE official_act SET updated_by = NULL
WHERE updated_by IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.id = official_act.updated_by);

ALTER TABLE endereco
    ADD CONSTRAINT fk_endereco_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE pessoa
    ADD CONSTRAINT fk_pessoa_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE pessoa_history
    ADD CONSTRAINT fk_pessoa_history_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE person_note
    ADD CONSTRAINT fk_person_note_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE person_note_history
    ADD CONSTRAINT fk_person_note_history_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;

ALTER TABLE official_act
    ADD CONSTRAINT fk_official_act_updated_by
    FOREIGN KEY (updated_by) REFERENCES usuario(id) ON DELETE RESTRICT;
