package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycleQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CYCLE;

@Repository
public class EbdCycleRepository {

    @Autowired
    private DSLContext dsl;

    public EbdCycle insert(String name, LocalDate startDate, LocalDate endDate,
                           boolean active, Long updatedBy) {
        Record rec = dsl.insertInto(EBD_CYCLE)
                .set(writableColumns(name, startDate, endDate, active, updatedBy))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public void update(Long id, String name, LocalDate startDate, LocalDate endDate,
                       boolean active, Long updatedBy) {
        dsl.update(EBD_CYCLE)
                .set(writableColumns(name, startDate, endDate, active, updatedBy))
                .where(EBD_CYCLE.ID.eq(id))
                .execute();
    }

    public boolean existsByName(String name, Long excludeId) {
        if (name == null) return false;
        Condition cond = EBD_CYCLE.NAME.eq(name);
        if (excludeId != null) cond = cond.and(EBD_CYCLE.ID.ne(excludeId));
        return dsl.fetchExists(dsl.selectOne().from(EBD_CYCLE).where(cond));
    }

    // Espelha o índice único parcial (só 1 ciclo active por vez) para devolver
    // 400 em PT em vez de deixar a violação de constraint virar 500 genérico.
    public boolean existsOtherActive(Long excludeId) {
        Condition cond = EBD_CYCLE.ACTIVE.isTrue();
        if (excludeId != null) cond = cond.and(EBD_CYCLE.ID.ne(excludeId));
        return dsl.fetchExists(dsl.selectOne().from(EBD_CYCLE).where(cond));
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
        if (query.active() != null) conditions.add(EBD_CYCLE.ACTIVE.eq(query.active()));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(String name, LocalDate startDate,
                                                   LocalDate endDate, boolean active, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_CYCLE.NAME, name);
        columns.put(EBD_CYCLE.START_DATE, DateTimeHelper.toDbDate(startDate));
        columns.put(EBD_CYCLE.END_DATE, DateTimeHelper.toDbDate(endDate));
        columns.put(EBD_CYCLE.ACTIVE, active);
        columns.put(EBD_CYCLE.UPDATED_BY, updatedBy);
        return columns;
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
