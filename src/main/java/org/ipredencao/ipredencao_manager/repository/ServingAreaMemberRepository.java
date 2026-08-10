package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.ServingAreaPositionKind;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportRow;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMember;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMemberQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.CATEGORIA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_MEMBER;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_POSITION;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_TEAM;
import static org.jooq.impl.DSL.case_;

@Repository
public class ServingAreaMemberRepository {

    @Autowired
    private DSLContext dsl;

    // Retorna o id gerado; o service recarrega o vínculo (com JOINs) via find.
    public Long insert(Long servingAreaId, Long personId, Long positionId, Long teamId,
                       LocalDate startDate, Long updatedBy) {
        return dsl.insertInto(SERVING_AREA_MEMBER)
                .set(writableColumns(servingAreaId, personId, positionId, teamId, startDate, updatedBy))
                .returning(SERVING_AREA_MEMBER.ID)
                .fetchOne(SERVING_AREA_MEMBER.ID);
    }

    public void update(Long id, Long servingAreaId, Long personId, Long positionId, Long teamId,
                       LocalDate startDate, Long updatedBy) {
        dsl.update(SERVING_AREA_MEMBER)
                .set(writableColumns(servingAreaId, personId, positionId, teamId, startDate, updatedBy))
                .where(SERVING_AREA_MEMBER.ID.eq(id))
                .execute();
    }

    public void delete(Long id) {
        dsl.deleteFrom(SERVING_AREA_MEMBER).where(SERVING_AREA_MEMBER.ID.eq(id)).execute();
    }

    // Consulta genérica: cobre um vínculo por id, membros da área, vínculos da
    // pessoa, supervisores e a base do relatório.
    public List<ServingAreaMember> find(ServingAreaMemberQuery query) {
        Field<Integer> kindOrder = case_(SERVING_AREA_POSITION.KIND)
                .when(ServingAreaPositionKind.SUPERVISION, 0)
                .when(ServingAreaPositionKind.COORDINATION, 1)
                .otherwise(2);
        return baseSelect()
                .where(QueryConditions.reduceToAnd(buildConditions(query)))
                .orderBy(SERVING_AREA.NAME.asc(), kindOrder.asc(), SERVING_AREA_POSITION.NAME.asc(),
                        PESSOA.NOME.asc(), SERVING_AREA_MEMBER.ID.asc())
                .fetch(this::fromRecord);
    }

    // Vínculo idêntico (mesma área/pessoa/cargo/equipe).
    public boolean existsDuplicate(Long servingAreaId, Long personId, Long positionId, Long teamId, Long excludeId) {
        Condition cond = SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(servingAreaId)
                .and(SERVING_AREA_MEMBER.PERSON_ID.eq(personId))
                .and(SERVING_AREA_MEMBER.POSITION_ID.eq(positionId))
                .and(teamId == null ? SERVING_AREA_MEMBER.TEAM_ID.isNull() : SERVING_AREA_MEMBER.TEAM_ID.eq(teamId));
        if (excludeId != null) cond = cond.and(SERVING_AREA_MEMBER.ID.ne(excludeId));
        return dsl.fetchExists(dsl.selectOne().from(SERVING_AREA_MEMBER).where(cond));
    }

    // Pessoas candidatas ao relatório (filtro de categoria/campus), sem vínculos.
    public List<ParticipationReportRow> findReportPeople(List<Long> categoryIds, String campus) {
        List<Condition> conditions = new ArrayList<>();
        if (categoryIds != null && !categoryIds.isEmpty()) conditions.add(PESSOA.CATEGORIA_ID.in(categoryIds));
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.CAMPUS, campus);

        return dsl.select(PESSOA.PESSOA_ID, PESSOA.NOME, PESSOA.CATEGORIA_ID, CATEGORIA.NOME.as("categoria_nome"))
                .from(PESSOA)
                .leftJoin(CATEGORIA).on(CATEGORIA.ID.eq(PESSOA.CATEGORIA_ID))
                .where(QueryConditions.reduceToAnd(conditions))
                .orderBy(PESSOA.NOME.asc())
                .fetch(r -> new ParticipationReportRow(
                        r.get(PESSOA.PESSOA_ID),
                        r.get(PESSOA.NOME),
                        r.get(PESSOA.CATEGORIA_ID),
                        r.get("categoria_nome", String.class),
                        new ArrayList<>()));
    }

    private List<Condition> buildConditions(ServingAreaMemberQuery q) {
        List<Condition> conditions = new ArrayList<>();
        if (q == null) return conditions;

        if (q.id() != null) conditions.add(SERVING_AREA_MEMBER.ID.eq(q.id()));
        if (q.servingAreaId() != null) conditions.add(SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(q.servingAreaId()));
        if (q.personId() != null) conditions.add(SERVING_AREA_MEMBER.PERSON_ID.eq(q.personId()));
        if (q.positionId() != null) conditions.add(SERVING_AREA_MEMBER.POSITION_ID.eq(q.positionId()));
        if (q.teamId() != null) conditions.add(SERVING_AREA_MEMBER.TEAM_ID.eq(q.teamId()));
        if (q.kind() != null) conditions.add(SERVING_AREA_POSITION.KIND.eq(ServingAreaPositionKind.valueOf(q.kind().name())));
        if (q.categoryIds() != null && !q.categoryIds().isEmpty()) conditions.add(PESSOA.CATEGORIA_ID.in(q.categoryIds()));
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.CAMPUS, q.campus());
        
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long servingAreaId, Long personId, Long positionId, Long teamId,
                                                  LocalDate startDate, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(SERVING_AREA_MEMBER.SERVING_AREA_ID, servingAreaId);
        columns.put(SERVING_AREA_MEMBER.PERSON_ID, personId);
        columns.put(SERVING_AREA_MEMBER.POSITION_ID, positionId);
        columns.put(SERVING_AREA_MEMBER.TEAM_ID, teamId);
        columns.put(SERVING_AREA_MEMBER.START_DATE, DateTimeHelper.toDbDate(startDate));
        columns.put(SERVING_AREA_MEMBER.UPDATED_BY, updatedBy);
        return columns;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(
                        SERVING_AREA_MEMBER.asterisk(),
                        SERVING_AREA.NAME.as("area_name"),
                        SERVING_AREA_POSITION.NAME.as("position_name"),
                        SERVING_AREA_POSITION.KIND,
                        SERVING_AREA_TEAM.NAME.as("team_name"),
                        PESSOA.NOME.as("person_name"),
                        PESSOA.CATEGORIA_ID,
                        CATEGORIA.NOME.as("person_categoria_nome"))
                .from(SERVING_AREA_MEMBER)
                .join(SERVING_AREA).on(SERVING_AREA.ID.eq(SERVING_AREA_MEMBER.SERVING_AREA_ID))
                .join(SERVING_AREA_POSITION).on(SERVING_AREA_POSITION.ID.eq(SERVING_AREA_MEMBER.POSITION_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(SERVING_AREA_MEMBER.PERSON_ID))
                .leftJoin(SERVING_AREA_TEAM).on(SERVING_AREA_TEAM.ID.eq(SERVING_AREA_MEMBER.TEAM_ID))
                .leftJoin(CATEGORIA).on(CATEGORIA.ID.eq(PESSOA.CATEGORIA_ID));
    }

    private ServingAreaMember fromRecord(Record r) {
        ServingAreaPositionKind kind = r.get(SERVING_AREA_POSITION.KIND);
        return new ServingAreaMember(
                r.get(SERVING_AREA_MEMBER.ID),
                r.get(SERVING_AREA_MEMBER.SERVING_AREA_ID),
                r.get(SERVING_AREA_MEMBER.PERSON_ID),
                r.get(SERVING_AREA_MEMBER.POSITION_ID),
                r.get(SERVING_AREA_MEMBER.TEAM_ID),
                DateTimeHelper.fromDbDate(r.get(SERVING_AREA_MEMBER.START_DATE)),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_MEMBER.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(SERVING_AREA_MEMBER.UPDATED_AT)),
                r.get(SERVING_AREA_MEMBER.UPDATED_BY),
                r.get("area_name", String.class),
                r.get("person_name", String.class),
                r.get(PESSOA.CATEGORIA_ID),
                r.get("person_categoria_nome", String.class),
                r.get("position_name", String.class),
                kind != null ? ServingAreaPositionKindEnum.valueOf(kind.name()) : null,
                r.get("team_name", String.class));
    }
}
