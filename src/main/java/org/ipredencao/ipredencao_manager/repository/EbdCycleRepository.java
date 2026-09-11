package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.EbdCycleRecord;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycleQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CYCLE;

@Repository
public class EbdCycleRepository {

    @Autowired
    private DSLContext dsl;

    public EbdCycle insert(String name, LocalDate startDate, LocalDate endDate,
                           boolean active, Long updatedBy) {
        Record rec = dsl.insertInto(EBD_CYCLE)
                .set(toRecord(name, startDate, endDate, active, updatedBy))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public void update(Long id, String name, LocalDate startDate, LocalDate endDate,
                       boolean active, Long updatedBy) {
        dsl.update(EBD_CYCLE)
                .set(toRecord(name, startDate, endDate, active, updatedBy))
                .where(EBD_CYCLE.ID.eq(id))
                .execute();
    }

    public List<EbdCycle> find(EbdCycleQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.selectFrom(EBD_CYCLE)
                .where(where)
                .orderBy(EBD_CYCLE.START_DATE.desc().nullsLast(), EBD_CYCLE.ID.desc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(EbdCycleQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_CYCLE.ID.eq(query.id()));
        if (query.excludeId() != null) conditions.add(EBD_CYCLE.ID.ne(query.excludeId()));
        if (query.name() != null) conditions.add(EBD_CYCLE.NAME.eq(query.name()));
        if (query.active() != null) conditions.add(EBD_CYCLE.ACTIVE.eq(query.active()));
        return conditions;
    }

    private EbdCycleRecord toRecord(String name, LocalDate startDate, LocalDate endDate,
                                    boolean active, Long updatedBy) {
        EbdCycleRecord rec = new EbdCycleRecord();
        rec.setName(name);
        rec.setStartDate(DateTimeHelper.toDbDate(startDate));
        rec.setEndDate(DateTimeHelper.toDbDate(endDate));
        rec.setActive(active);
        rec.setUpdatedBy(updatedBy);
        return rec;
    }

    private EbdCycle fromRecord(Record r) {
        return new EbdCycle(
                r.get(EBD_CYCLE.ID),
                r.get(EBD_CYCLE.NAME),
                DateTimeHelper.fromDbDate(r.get(EBD_CYCLE.START_DATE)),
                DateTimeHelper.fromDbDate(r.get(EBD_CYCLE.END_DATE)),
                r.get(EBD_CYCLE.ACTIVE),
                DateTimeHelper.fromDb(r.get(EBD_CYCLE.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_CYCLE.UPDATED_AT)),
                r.get(EBD_CYCLE.UPDATED_BY));
    }
}
