-- Ata do conselho: number e date moram aqui. official_act.minute_number vira FK.

CREATE TABLE IF NOT EXISTS minute (
    number VARCHAR(64) PRIMARY KEY,
    date DATE,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_minute_number_not_blank CHECK (btrim(number) <> '')
);

CREATE TRIGGER trigger_minute_updated_at
    BEFORE UPDATE ON minute
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

UPDATE official_act
SET minute_number = NULL
WHERE minute_number IS NOT NULL AND btrim(minute_number) = '';

UPDATE official_act
SET minute_number = btrim(minute_number)
WHERE minute_number IS NOT NULL AND minute_number <> btrim(minute_number);

INSERT INTO minute (number, date)
SELECT minute_number, MAX(minute_date)
FROM official_act
WHERE minute_number IS NOT NULL
GROUP BY minute_number;

ALTER TABLE official_act DROP COLUMN minute_date;

ALTER TABLE official_act
    ADD CONSTRAINT fk_official_act_minute_number
    FOREIGN KEY (minute_number) REFERENCES minute(number)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT;
