package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolCycleRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolCycle;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolCycleQuery;
import org.ipredencao.ipredencao_manager.repository.QueryConditions;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_CYCLE;

@Repository
public class BibleSchoolCycleRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolCycle insert(BibleSchoolCycle cycle) {
        Record rec = dsl.insertInto(BIBLE_SCHOOL_CYCLE)
                .set(toRecord(cycle))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public BibleSchoolCycle update(BibleSchoolCycle cycle) {
        dsl.update(BIBLE_SCHOOL_CYCLE)
                .set(toRecord(cycle))
                .where(BIBLE_SCHOOL_CYCLE.ID.eq(cycle.id()))
                .execute();
        return find(BibleSchoolCycleQuery.builder().id(cycle.id()).build()).getFirst();
    }

    public List<BibleSchoolCycle> find(BibleSchoolCycleQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.selectFrom(BIBLE_SCHOOL_CYCLE)
                .where(where)
                .orderBy(BIBLE_SCHOOL_CYCLE.START_DATE.desc().nullsLast(), BIBLE_SCHOOL_CYCLE.ID.desc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(BibleSchoolCycleQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(BIBLE_SCHOOL_CYCLE.ID.eq(query.id()));
        if (query.excludeId() != null) conditions.add(BIBLE_SCHOOL_CYCLE.ID.ne(query.excludeId()));
        if (query.name() != null) conditions.add(BIBLE_SCHOOL_CYCLE.NAME.eq(query.name()));
        if (query.active() != null) conditions.add(BIBLE_SCHOOL_CYCLE.ACTIVE.eq(query.active()));
        return conditions;
    }

    private BibleSchoolCycleRecord toRecord(BibleSchoolCycle cycle) {
        BibleSchoolCycleRecord rec = new BibleSchoolCycleRecord();
        rec.setName(cycle.name());
        rec.setStartDate(DateTimeHelper.toDbDate(cycle.startDate()));
        rec.setEndDate(DateTimeHelper.toDbDate(cycle.endDate()));
        rec.setActive(Boolean.TRUE.equals(cycle.active()));
        rec.setUpdatedBy(cycle.updatedBy());
        return rec;
    }

    private BibleSchoolCycle fromRecord(Record r) {
        return new BibleSchoolCycle(
                r.get(BIBLE_SCHOOL_CYCLE.ID),
                r.get(BIBLE_SCHOOL_CYCLE.NAME),
                DateTimeHelper.fromDbDate(r.get(BIBLE_SCHOOL_CYCLE.START_DATE)),
                DateTimeHelper.fromDbDate(r.get(BIBLE_SCHOOL_CYCLE.END_DATE)),
                r.get(BIBLE_SCHOOL_CYCLE.ACTIVE),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_CYCLE.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_CYCLE.UPDATED_AT)),
                r.get(BIBLE_SCHOOL_CYCLE.UPDATED_BY));
    }
}
