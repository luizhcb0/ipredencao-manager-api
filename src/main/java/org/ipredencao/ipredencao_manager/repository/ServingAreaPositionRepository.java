package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.ServingAreaPositionKind;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPosition;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionQuery;
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
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_POSITION;

@Repository
public class ServingAreaPositionRepository {

    @Autowired
    private DSLContext dsl;

    // Sem campos derivados: insert e update retornam a própria linha (RETURNING).
    public ServingAreaPosition insert(Long servingAreaId, String name, ServingAreaPositionKindEnum kind,
                                      Integer sortOrder, boolean active, Long updatedBy) {
        Record rec = dsl.insertInto(SERVING_AREA_POSITION)
                .set(writableColumns(servingAreaId, name, kind, sortOrder, active, updatedBy))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public ServingAreaPosition update(Long id, Long servingAreaId, String name, ServingAreaPositionKindEnum kind,
                                     Integer sortOrder, boolean active, Long updatedBy) {
        Record rec = dsl.update(SERVING_AREA_POSITION)
                .set(writableColumns(servingAreaId, name, kind, sortOrder, active, updatedBy))
                .where(SERVING_AREA_POSITION.ID.eq(id))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public void delete(Long id) {
        dsl.deleteFrom(SERVING_AREA_POSITION).where(SERVING_AREA_POSITION.ID.eq(id)).execute();
    }

    public List<ServingAreaPosition> find(ServingAreaPositionQuery query) {
        return dsl.selectFrom(SERVING_AREA_POSITION)
                .where(QueryConditions.reduceToAnd(buildConditions(query)))
                .orderBy(SERVING_AREA_POSITION.SORT_ORDER.asc(), SERVING_AREA_POSITION.ID.asc())
                .fetch(this::fromRecord);
    }

    // Vínculo presente referenciando o cargo.
    public boolean hasMembers(Long positionId) {
        return dsl.fetchExists(dsl.selectOne()
                .from(SERVING_AREA_MEMBER)
                .where(SERVING_AREA_MEMBER.POSITION_ID.eq(positionId)));
    }

    private List<Condition> buildConditions(ServingAreaPositionQuery q) {
        List<Condition> conditions = new ArrayList<>();
        if (q == null) return conditions;
        if (q.id() != null) conditions.add(SERVING_AREA_POSITION.ID.eq(q.id()));
        if (q.servingAreaId() != null) conditions.add(SERVING_AREA_POSITION.SERVING_AREA_ID.eq(q.servingAreaId()));
        if (q.active() != null) conditions.add(SERVING_AREA_POSITION.ACTIVE.eq(q.active()));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long servingAreaId, String name, ServingAreaPositionKindEnum kind,
                                                  Integer sortOrder, boolean active, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(SERVING_AREA_POSITION.SERVING_AREA_ID, servingAreaId);
        columns.put(SERVING_AREA_POSITION.NAME, name);
        columns.put(SERVING_AREA_POSITION.KIND, ServingAreaPositionKind.valueOf(kind.name()));
        columns.put(SERVING_AREA_POSITION.SORT_ORDER, sortOrder);
        columns.put(SERVING_AREA_POSITION.ACTIVE, active);
        columns.put(SERVING_AREA_POSITION.UPDATED_BY, updatedBy);
        return columns;
    }

    private ServingAreaPosition fromRecord(Record r) {
        ServingAreaPositionKind kind = r.get(SERVING_AREA_POSITION.KIND);
        return new ServingAreaPosition(
                r.get(SERVING_AREA_POSITION.ID),
                r.get(SERVING_AREA_POSITION.SERVING_AREA_ID),
                r.get(SERVING_AREA_POSITION.NAME),
                kind != null ? ServingAreaPositionKindEnum.valueOf(kind.name()) : null,
                r.get(SERVING_AREA_POSITION.SORT_ORDER),
                r.get(SERVING_AREA_POSITION.ACTIVE),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_POSITION.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_POSITION.UPDATED_AT)),
                r.get(SERVING_AREA_POSITION.UPDATED_BY));
    }
}
