package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.EbdClassStatus;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CLASS;
import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CYCLE;

@Repository
public class EbdClassRepository {

    @Autowired
    private DSLContext dsl;

    public EbdClass insert(Long cycleId, String name, String description,
                           EbdClassStatusEnum status, Long updatedBy) {
        Record rec = dsl.insertInto(EBD_CLASS)
                .set(writableColumns(cycleId, name, description, status, updatedBy))
                .returning(EBD_CLASS.ID)
                .fetchOne();
        return find(EbdClassQuery.builder().id(rec.get(EBD_CLASS.ID)).build()).getFirst();
    }

    public void update(Long id, Long cycleId, String name, String description,
                       EbdClassStatusEnum status, Long updatedBy) {
        dsl.update(EBD_CLASS)
                .set(writableColumns(cycleId, name, description, status, updatedBy))
                .where(EBD_CLASS.ID.eq(id))
                .execute();
    }

    public List<EbdClass> find(EbdClassQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        PaginationParameters pagination = query != null ? query.pagination() : null;
        SelectConditionStep<Record> step = baseSelect().where(where);
        if (pagination != null) {
            pagination.applyDefaults();
            return step.orderBy(EBD_CLASS.NAME.asc(), EBD_CLASS.ID.asc())
                    .limit(pagination.getLimit())
                    .offset(pagination.getOffset())
                    .fetch(this::fromRecord);
        }
        return step.orderBy(EBD_CLASS.NAME.asc(), EBD_CLASS.ID.asc()).fetch(this::fromRecord);
    }

    public int count(EbdClassQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        Integer total = dsl.selectCount().from(EBD_CLASS).where(where).fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    private List<Condition> buildConditions(EbdClassQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_CLASS.ID.eq(query.id()));
        QueryConditions.addUnaccentedLike(conditions, EBD_CLASS.NAME, query.name());
        if (query.cycleId() != null) conditions.add(EBD_CLASS.CYCLE_ID.eq(query.cycleId()));
        if (query.status() != null) conditions.add(EBD_CLASS.STATUS.eq(EbdClassStatus.valueOf(query.status().name())));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long cycleId, String name, String description,
                                                   EbdClassStatusEnum status, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_CLASS.CYCLE_ID, cycleId);
        columns.put(EBD_CLASS.NAME, name);
        columns.put(EBD_CLASS.DESCRIPTION, description);
        columns.put(EBD_CLASS.STATUS, EbdClassStatus.valueOf(status.name()));
        columns.put(EBD_CLASS.UPDATED_BY, updatedBy);
        return columns;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(EBD_CLASS.asterisk(), EBD_CYCLE.NAME.as("cycle_name"))
                .from(EBD_CLASS)
                .leftJoin(EBD_CYCLE).on(EBD_CYCLE.ID.eq(EBD_CLASS.CYCLE_ID));
    }

    private EbdClass fromRecord(Record r) {
        EbdClassStatus status = r.get(EBD_CLASS.STATUS);
        return new EbdClass(
                r.get(EBD_CLASS.ID),
                r.get(EBD_CLASS.CYCLE_ID),
                r.get("cycle_name", String.class),
                r.get(EBD_CLASS.NAME),
                r.get(EBD_CLASS.DESCRIPTION),
                status != null ? EbdClassStatusEnum.valueOf(status.name()) : null,
                DateTimeHelper.fromDb(r.get(EBD_CLASS.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_CLASS.UPDATED_AT)),
                r.get(EBD_CLASS.UPDATED_BY),
                null);
    }
}
