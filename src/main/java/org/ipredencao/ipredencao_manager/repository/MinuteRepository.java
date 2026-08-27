package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.official_act.Minute;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import static org.ipredencao.ipredencao_manager.jooq.Tables.MINUTE;

@Repository
public class MinuteRepository {

    @Autowired
    private DSLContext dsl;

    public Minute findByNumber(String number) {
        if (number == null || number.isBlank()) return null;
        Record record = dsl.selectFrom(MINUTE)
                .where(MINUTE.NUMBER.eq(number))
                .fetchOne();
        return record != null ? fromRecord(record) : null;
    }

    public Minute insert(Minute minute) {
        Record record = dsl.insertInto(MINUTE)
                .set(MINUTE.NUMBER, minute.number())
                .set(MINUTE.DATE, DateTimeHelper.toDbDate(minute.date()))
                .returning()
                .fetchOne();
        return fromRecord(record);
    }

    public Minute update(Minute minute) {
        Record record = dsl.update(MINUTE)
                .set(MINUTE.DATE, DateTimeHelper.toDbDate(minute.date()))
                .where(MINUTE.NUMBER.eq(minute.number()))
                .returning()
                .fetchOne();
        return record != null ? fromRecord(record) : null;
    }

    private Minute fromRecord(Record record) {
        return new Minute(
                record.get(MINUTE.NUMBER),
                DateTimeHelper.fromDbDate(record.get(MINUTE.DATE)));
    }
}
