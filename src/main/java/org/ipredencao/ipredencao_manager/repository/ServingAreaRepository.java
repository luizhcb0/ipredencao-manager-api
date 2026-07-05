package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.ServingAreaPositionKind;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingArea;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_MEMBER;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_POSITION;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_TEAM;

@Repository
public class ServingAreaRepository {

    @Autowired
    private DSLContext dsl;

    public ServingArea insert(String name, String description, String whatsappUrl,
                              String coordinationWhatsappUrl, boolean active, Long updatedBy) {
        Record rec = dsl.insertInto(SERVING_AREA)
                .set(writableColumns(name, description, whatsappUrl, coordinationWhatsappUrl, active, updatedBy))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public void update(Long id, String name, String description, String whatsappUrl,
                       String coordinationWhatsappUrl, boolean active, Long updatedBy) {
        dsl.update(SERVING_AREA)
                .set(writableColumns(name, description, whatsappUrl, coordinationWhatsappUrl, active, updatedBy))
                .where(SERVING_AREA.ID.eq(id))
                .execute();
    }

    public void delete(Long id) {
        dsl.deleteFrom(SERVING_AREA).where(SERVING_AREA.ID.eq(id)).execute();
    }

    public boolean existsByName(String name, Long excludeId) {
        if (name == null) return false;
        Condition cond = SERVING_AREA.NAME.eq(name);
        if (excludeId != null) cond = cond.and(SERVING_AREA.ID.ne(excludeId));
        return dsl.fetchExists(dsl.selectOne().from(SERVING_AREA).where(cond));
    }

    public List<ServingArea> find(ServingAreaQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        PaginationParameters pagination = query != null ? query.pagination() : null;
        SelectConditionStep<Record> step = baseSelect().where(where);
        if (pagination != null) {
            pagination.applyDefaults();
            return step.orderBy(SERVING_AREA.NAME.asc(), SERVING_AREA.ID.asc())
                    .limit(pagination.getLimit())
                    .offset(pagination.getOffset())
                    .fetch(this::fromRecord);
        }
        return step.orderBy(SERVING_AREA.NAME.asc(), SERVING_AREA.ID.asc()).fetch(this::fromRecord);
    }

    public int count(ServingAreaQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        Integer total = dsl.selectCount().from(SERVING_AREA).where(where).fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    private Map<Field<?>, Object> writableColumns(String name, String description, String whatsappUrl,
                                                  String coordinationWhatsappUrl, boolean active, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(SERVING_AREA.NAME, name);
        columns.put(SERVING_AREA.DESCRIPTION, description);
        columns.put(SERVING_AREA.WHATSAPP_URL, whatsappUrl);
        columns.put(SERVING_AREA.COORDINATION_WHATSAPP_URL, coordinationWhatsappUrl);
        columns.put(SERVING_AREA.ACTIVE, active);
        columns.put(SERVING_AREA.UPDATED_BY, updatedBy);
        return columns;
    }

    private List<Condition> buildConditions(ServingAreaQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;

        if (query.id() != null) conditions.add(SERVING_AREA.ID.eq(query.id()));
        QueryConditions.addUnaccentedLike(conditions, SERVING_AREA.NAME, query.name());
        if (query.active() != null) {
            conditions.add(SERVING_AREA.ACTIVE.eq(query.active()));
        }
        if (query.supervisorPersonId() != null) {
            conditions.add(DSL.exists(dsl.selectOne()
                    .from(SERVING_AREA_MEMBER)
                    .join(SERVING_AREA_POSITION).on(SERVING_AREA_POSITION.ID.eq(SERVING_AREA_MEMBER.POSITION_ID))
                    .where(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(SERVING_AREA.ID))
                    .and(SERVING_AREA_POSITION.KIND.eq(ServingAreaPositionKind.SUPERVISION))
                    .and(SERVING_AREA_MEMBER.PERSON_ID.eq(query.supervisorPersonId()))));
        }
        if (query.personId() != null) {
            conditions.add(DSL.exists(dsl.selectOne()
                    .from(SERVING_AREA_MEMBER)
                    .where(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(SERVING_AREA.ID))
                    .and(SERVING_AREA_MEMBER.PERSON_ID.eq(query.personId()))));
        }
        return conditions;
    }

    private SelectJoinStep<Record> baseSelect() {
        Field<Long> supervisorPersonId = DSL.field(dsl.select(SERVING_AREA_MEMBER.PERSON_ID)
                .from(SERVING_AREA_MEMBER)
                .join(SERVING_AREA_POSITION).on(SERVING_AREA_POSITION.ID.eq(SERVING_AREA_MEMBER.POSITION_ID))
                .where(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(SERVING_AREA.ID))
                .and(SERVING_AREA_POSITION.KIND.eq(ServingAreaPositionKind.SUPERVISION))
                .orderBy(SERVING_AREA_MEMBER.ID.desc())
                .limit(1)).as("supervisor_person_id");

        Field<String> supervisorName = DSL.field(dsl.select(PESSOA.NOME)
                .from(SERVING_AREA_MEMBER)
                .join(SERVING_AREA_POSITION).on(SERVING_AREA_POSITION.ID.eq(SERVING_AREA_MEMBER.POSITION_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(SERVING_AREA_MEMBER.PERSON_ID))
                .where(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(SERVING_AREA.ID))
                .and(SERVING_AREA_POSITION.KIND.eq(ServingAreaPositionKind.SUPERVISION))
                .orderBy(SERVING_AREA_MEMBER.ID.desc())
                .limit(1)).as("supervisor_name");

        Field<Integer> teamCount = DSL.field(dsl.selectCount()
                .from(SERVING_AREA_TEAM)
                .where(SERVING_AREA_TEAM.SERVING_AREA_ID.eq(SERVING_AREA.ID))).as("team_count");

        Field<Integer> memberCount = kindMemberCount(ServingAreaPositionKind.MEMBERSHIP, "member_count");
        Field<Integer> coordinatorCount = kindMemberCount(ServingAreaPositionKind.COORDINATION, "coordinator_count");

        return dsl.select(
                        SERVING_AREA.asterisk(),
                        supervisorPersonId,
                        supervisorName,
                        teamCount,
                        memberCount,
                        coordinatorCount)
                .from(SERVING_AREA);
    }

    private Field<Integer> kindMemberCount(ServingAreaPositionKind kind, String alias) {
        return DSL.field(dsl.selectCount()
                .from(SERVING_AREA_MEMBER)
                .join(SERVING_AREA_POSITION).on(SERVING_AREA_POSITION.ID.eq(SERVING_AREA_MEMBER.POSITION_ID))
                .where(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(SERVING_AREA.ID))
                .and(SERVING_AREA_POSITION.KIND.eq(kind))).as(alias);
    }

    private ServingArea fromRecord(Record r) {
        return new ServingArea(
                r.get(SERVING_AREA.ID),
                r.get(SERVING_AREA.NAME),
                r.get(SERVING_AREA.DESCRIPTION),
                r.get(SERVING_AREA.WHATSAPP_URL),
                r.get(SERVING_AREA.COORDINATION_WHATSAPP_URL),
                r.get(SERVING_AREA.ACTIVE),
                DateTimeHelper.fromDb(r.get(SERVING_AREA.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(SERVING_AREA.UPDATED_AT)),
                r.get(SERVING_AREA.UPDATED_BY),
                derived(r, "supervisor_person_id", Long.class, null),
                derived(r, "supervisor_name", String.class, null),
                derived(r, "team_count", Integer.class, 0),
                derived(r, "member_count", Integer.class, 0),
                derived(r, "coordinator_count", Integer.class, 0),
                null, null, null);
    }

    private static <T> T derived(Record r, String name, Class<T> type, T dflt) {
        return r.field(name) != null ? r.get(name, type) : dflt;
    }
}
