package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.enums.BibleSchoolClassStatus;
import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolClassRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClass;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.repository.QueryConditions;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_CLASS;
import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_CYCLE;

@Repository
public class BibleSchoolClassRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolClass insert(BibleSchoolClass bibleSchoolClass) {
        Record rec = dsl.insertInto(BIBLE_SCHOOL_CLASS)
                .set(toRecord(bibleSchoolClass))
                .returning(BIBLE_SCHOOL_CLASS.ID)
                .fetchOne();
        return find(BibleSchoolClassQuery.builder().id(rec.get(BIBLE_SCHOOL_CLASS.ID)).build()).getFirst();
    }

    public BibleSchoolClass update(BibleSchoolClass bibleSchoolClass) {
        dsl.update(BIBLE_SCHOOL_CLASS)
                .set(toRecord(bibleSchoolClass))
                .where(BIBLE_SCHOOL_CLASS.ID.eq(bibleSchoolClass.id()))
                .execute();
        return find(BibleSchoolClassQuery.builder().id(bibleSchoolClass.id()).build()).getFirst();
    }

    public List<BibleSchoolClass> find(BibleSchoolClassQuery query) {
        return fetch(buildConditions(query), query != null ? query.pagination() : null);
    }

    public int count(BibleSchoolClassQuery query) {
        return countWhere(buildConditions(query));
    }

    private List<Condition> buildConditions(BibleSchoolClassQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(BIBLE_SCHOOL_CLASS.ID.eq(query.id()));
        if (query.ids() != null) {
            conditions.add(query.ids().isEmpty()
                    ? DSL.falseCondition()
                    : BIBLE_SCHOOL_CLASS.ID.in(query.ids()));
        }
        QueryConditions.addUnaccentedLike(conditions, BIBLE_SCHOOL_CLASS.NAME, query.name());
        if (query.cycleId() != null) conditions.add(BIBLE_SCHOOL_CLASS.CYCLE_ID.eq(query.cycleId()));
        if (query.status() != null) {
            conditions.add(BIBLE_SCHOOL_CLASS.STATUS.eq(BibleSchoolClassStatus.valueOf(query.status().name())));
        }
        return conditions;
    }

    private List<BibleSchoolClass> fetch(List<Condition> conditions, PaginationParameters pagination) {
        SelectConditionStep<Record> step = baseSelect().where(QueryConditions.reduceToAnd(conditions));
        if (pagination != null) {
            pagination.applyDefaults();
            return step.orderBy(BIBLE_SCHOOL_CLASS.NAME.asc(), BIBLE_SCHOOL_CLASS.ID.asc())
                    .limit(pagination.getLimit())
                    .offset(pagination.getOffset())
                    .fetch(this::fromRecord);
        }
        return step.orderBy(BIBLE_SCHOOL_CLASS.NAME.asc(), BIBLE_SCHOOL_CLASS.ID.asc()).fetch(this::fromRecord);
    }

    private int countWhere(List<Condition> conditions) {
        Integer total = dsl.selectCount().from(BIBLE_SCHOOL_CLASS)
                .where(QueryConditions.reduceToAnd(conditions))
                .fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    private BibleSchoolClassRecord toRecord(BibleSchoolClass bibleSchoolClass) {
        BibleSchoolClassRecord rec = new BibleSchoolClassRecord();
        rec.setCycleId(bibleSchoolClass.cycleId());
        rec.setName(bibleSchoolClass.name());
        rec.setDescription(bibleSchoolClass.description());
        rec.setStatus(BibleSchoolClassStatus.valueOf(bibleSchoolClass.status().name()));
        rec.setUpdatedBy(bibleSchoolClass.updatedBy());
        return rec;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(BIBLE_SCHOOL_CLASS.asterisk(), BIBLE_SCHOOL_CYCLE.NAME.as("cycle_name"))
                .from(BIBLE_SCHOOL_CLASS)
                .leftJoin(BIBLE_SCHOOL_CYCLE).on(BIBLE_SCHOOL_CYCLE.ID.eq(BIBLE_SCHOOL_CLASS.CYCLE_ID));
    }

    private BibleSchoolClass fromRecord(Record r) {
        BibleSchoolClassStatus status = r.get(BIBLE_SCHOOL_CLASS.STATUS);
        return new BibleSchoolClass(
                r.get(BIBLE_SCHOOL_CLASS.ID),
                r.get(BIBLE_SCHOOL_CLASS.CYCLE_ID),
                r.get("cycle_name", String.class),
                r.get(BIBLE_SCHOOL_CLASS.NAME),
                r.get(BIBLE_SCHOOL_CLASS.DESCRIPTION),
                status != null ? BibleSchoolClassStatusEnum.valueOf(status.name()) : null,
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_CLASS.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_CLASS.UPDATED_AT)),
                r.get(BIBLE_SCHOOL_CLASS.UPDATED_BY),
                null);
    }
}
