package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeam;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeamQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_MEMBER;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_TEAM;

@Repository
public class ServingAreaTeamRepository {

    @Autowired
    private DSLContext dsl;

    public ServingAreaTeam insert(Long servingAreaId, String name, String description,
                                  String whatsappUrl, boolean active, Long updatedBy) {
        Record rec = dsl.insertInto(SERVING_AREA_TEAM)
                .set(writableColumns(servingAreaId, name, description, whatsappUrl, active, updatedBy))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public ServingAreaTeam update(Long id, Long servingAreaId, String name, String description,
                                  String whatsappUrl, boolean active, Long updatedBy) {
        Record rec = dsl.update(SERVING_AREA_TEAM)
                .set(writableColumns(servingAreaId, name, description, whatsappUrl, active, updatedBy))
                .where(SERVING_AREA_TEAM.ID.eq(id))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public void delete(Long id) {
        dsl.deleteFrom(SERVING_AREA_TEAM).where(SERVING_AREA_TEAM.ID.eq(id)).execute();
    }

    public List<ServingAreaTeam> find(ServingAreaTeamQuery query) {
        return dsl.selectFrom(SERVING_AREA_TEAM)
                .where(QueryConditions.reduceToAnd(buildConditions(query)))
                .orderBy(SERVING_AREA_TEAM.NAME.asc(), SERVING_AREA_TEAM.ID.asc())
                .fetch(this::fromRecord);
    }

    // Vínculo presente referenciando a equipe.
    public boolean hasMembers(Long teamId) {
        return dsl.fetchExists(dsl.selectOne()
                .from(SERVING_AREA_MEMBER)
                .where(SERVING_AREA_MEMBER.TEAM_ID.eq(teamId)));
    }

    private List<Condition> buildConditions(ServingAreaTeamQuery q) {
        List<Condition> conditions = new ArrayList<>();
        if (q == null) return conditions;
        if (q.id() != null) conditions.add(SERVING_AREA_TEAM.ID.eq(q.id()));
        if (q.servingAreaId() != null) conditions.add(SERVING_AREA_TEAM.SERVING_AREA_ID.eq(q.servingAreaId()));
        if (q.active() != null) conditions.add(SERVING_AREA_TEAM.ACTIVE.eq(q.active()));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long servingAreaId, String name, String description,
                                                  String whatsappUrl, boolean active, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(SERVING_AREA_TEAM.SERVING_AREA_ID, servingAreaId);
        columns.put(SERVING_AREA_TEAM.NAME, name);
        columns.put(SERVING_AREA_TEAM.DESCRIPTION, description);
        columns.put(SERVING_AREA_TEAM.WHATSAPP_URL, whatsappUrl);
        columns.put(SERVING_AREA_TEAM.ACTIVE, active);
        columns.put(SERVING_AREA_TEAM.UPDATED_BY, updatedBy);
        return columns;
    }

    private ServingAreaTeam fromRecord(Record r) {
        return new ServingAreaTeam(
                r.get(SERVING_AREA_TEAM.ID),
                r.get(SERVING_AREA_TEAM.SERVING_AREA_ID),
                r.get(SERVING_AREA_TEAM.NAME),
                r.get(SERVING_AREA_TEAM.DESCRIPTION),
                r.get(SERVING_AREA_TEAM.WHATSAPP_URL),
                r.get(SERVING_AREA_TEAM.ACTIVE),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_TEAM.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_TEAM.UPDATED_AT)),
                r.get(SERVING_AREA_TEAM.UPDATED_BY));
    }
}
