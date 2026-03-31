CREATE TABLE IF NOT EXISTS person_note (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);

CREATE TABLE IF NOT EXISTS person_note_history (
    history_id BIGSERIAL PRIMARY KEY,
    note_id BIGINT REFERENCES person_note(id) ON DELETE SET NULL,
    person_id BIGINT NOT NULL REFERENCES pessoa(pessoa_id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT
);


CREATE INDEX idx_person_note_pessoa_id ON person_note(person_id);
CREATE INDEX idx_person_note_history_note_id ON person_note_history(note_id);

CREATE TRIGGER trigger_person_note_updated_at
    BEFORE UPDATE ON person_note
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION insert_person_note_history()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO person_note_history (note_id, person_id, content, updated_by)
    VALUES (OLD.id, OLD.person_id, OLD.content, OLD.updated_by);
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER trigger_person_note_history
    BEFORE UPDATE ON person_note
    FOR EACH ROW
    EXECUTE FUNCTION insert_person_note_history();
