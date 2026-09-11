package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.EbdEnrollmentRole;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CLASS;
import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_ENROLLMENT;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;

@Repository
public class EbdEnrollmentRepository {

    @Autowired
    private DSLContext dsl;

    // Retorna o id gerado; o service recarrega o vínculo (com JOINs) via find.
    public Long insert(Long classId, Long personId, EbdEnrollmentRoleEnum role, Long updatedBy) {
        return dsl.insertInto(EBD_ENROLLMENT)
                .set(writableColumns(classId, personId, role, updatedBy))
                .returning(EBD_ENROLLMENT.ID)
                .fetchOne(EBD_ENROLLMENT.ID);
    }

    public void delete(Long id) {
        dsl.deleteFrom(EBD_ENROLLMENT).where(EBD_ENROLLMENT.ID.eq(id)).execute();
    }

    // Consulta genérica: cobre um vínculo por id, vínculos da turma, vínculos
    // da pessoa e a checagem "já existe esse vínculo" (find + isEmpty no
    // service, em vez de um exists dedicado).
    public List<EbdEnrollment> find(EbdEnrollmentQuery query) {
        return baseSelect()
                .where(QueryConditions.reduceToAnd(buildConditions(query)))
                .orderBy(EBD_ENROLLMENT.ROLE.asc(), PESSOA.NOME.asc(), EBD_ENROLLMENT.ID.asc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(EbdEnrollmentQuery q) {
        List<Condition> conditions = new ArrayList<>();
        if (q == null) return conditions;
        if (q.id() != null) conditions.add(EBD_ENROLLMENT.ID.eq(q.id()));
        if (q.classId() != null) conditions.add(EBD_ENROLLMENT.CLASS_ID.eq(q.classId()));
        if (q.personId() != null) conditions.add(EBD_ENROLLMENT.PERSON_ID.eq(q.personId()));
        if (q.role() != null) conditions.add(EBD_ENROLLMENT.ROLE.eq(EbdEnrollmentRole.valueOf(q.role().name())));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long classId, Long personId, EbdEnrollmentRoleEnum role,
                                                   Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_ENROLLMENT.CLASS_ID, classId);
        columns.put(EBD_ENROLLMENT.PERSON_ID, personId);
        columns.put(EBD_ENROLLMENT.ROLE, EbdEnrollmentRole.valueOf(role.name()));
        columns.put(EBD_ENROLLMENT.UPDATED_BY, updatedBy);
        return columns;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(
                        EBD_ENROLLMENT.asterisk(),
                        EBD_CLASS.NAME.as("class_name"),
                        PESSOA.NOME.as("person_name"))
                .from(EBD_ENROLLMENT)
                .join(EBD_CLASS).on(EBD_CLASS.ID.eq(EBD_ENROLLMENT.CLASS_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(EBD_ENROLLMENT.PERSON_ID));
    }

    private EbdEnrollment fromRecord(Record r) {
        EbdEnrollmentRole role = r.get(EBD_ENROLLMENT.ROLE);
        return new EbdEnrollment(
                r.get(EBD_ENROLLMENT.ID),
                r.get(EBD_ENROLLMENT.CLASS_ID),
                r.get("class_name", String.class),
                r.get(EBD_ENROLLMENT.PERSON_ID),
                r.get("person_name", String.class),
                role != null ? EbdEnrollmentRoleEnum.valueOf(role.name()) : null,
                DateTimeHelper.fromDb(r.get(EBD_ENROLLMENT.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_ENROLLMENT.UPDATED_AT)),
                r.get(EBD_ENROLLMENT.UPDATED_BY));
    }
}
