package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.ebd.EbdAttendance;
import org.ipredencao.ipredencao_manager.model.ebd.EbdAttendanceQuery;
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

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_ATTENDANCE;
import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_ENROLLMENT;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;

@Repository
public class EbdAttendanceRepository {

    @Autowired
    private DSLContext dsl;

    public Long insert(Long lessonId, Long enrollmentId, boolean present, boolean selfReported, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_ATTENDANCE.LESSON_ID, lessonId);
        columns.put(EBD_ATTENDANCE.ENROLLMENT_ID, enrollmentId);
        columns.put(EBD_ATTENDANCE.PRESENT, present);
        columns.put(EBD_ATTENDANCE.SELF_REPORTED, selfReported);
        columns.put(EBD_ATTENDANCE.UPDATED_BY, updatedBy);
        return dsl.insertInto(EBD_ATTENDANCE).set(columns).returning(EBD_ATTENDANCE.ID).fetchOne(EBD_ATTENDANCE.ID);
    }

    public void update(Long id, boolean present, boolean selfReported, Long updatedBy) {
        dsl.update(EBD_ATTENDANCE)
                .set(EBD_ATTENDANCE.PRESENT, present)
                .set(EBD_ATTENDANCE.SELF_REPORTED, selfReported)
                .set(EBD_ATTENDANCE.UPDATED_BY, updatedBy)
                .where(EBD_ATTENDANCE.ID.eq(id))
                .execute();
    }

    public boolean exists(Long lessonId, Long enrollmentId) {
        return dsl.fetchExists(dsl.selectOne().from(EBD_ATTENDANCE)
                .where(EBD_ATTENDANCE.LESSON_ID.eq(lessonId))
                .and(EBD_ATTENDANCE.ENROLLMENT_ID.eq(enrollmentId)));
    }

    public List<EbdAttendance> find(EbdAttendanceQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return baseSelect().where(where).orderBy(PESSOA.NOME.asc(), EBD_ATTENDANCE.ID.asc()).fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(EbdAttendanceQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_ATTENDANCE.ID.eq(query.id()));
        if (query.lessonId() != null) conditions.add(EBD_ATTENDANCE.LESSON_ID.eq(query.lessonId()));
        if (query.enrollmentId() != null) conditions.add(EBD_ATTENDANCE.ENROLLMENT_ID.eq(query.enrollmentId()));
        return conditions;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(EBD_ATTENDANCE.asterisk(), EBD_ENROLLMENT.PERSON_ID, PESSOA.NOME.as("person_name"))
                .from(EBD_ATTENDANCE)
                .join(EBD_ENROLLMENT).on(EBD_ENROLLMENT.ID.eq(EBD_ATTENDANCE.ENROLLMENT_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(EBD_ENROLLMENT.PERSON_ID));
    }

    private EbdAttendance fromRecord(Record r) {
        return new EbdAttendance(
                r.get(EBD_ATTENDANCE.ID),
                r.get(EBD_ATTENDANCE.LESSON_ID),
                r.get(EBD_ATTENDANCE.ENROLLMENT_ID),
                r.get(EBD_ENROLLMENT.PERSON_ID),
                r.get("person_name", String.class),
                r.get(EBD_ATTENDANCE.PRESENT),
                r.get(EBD_ATTENDANCE.SELF_REPORTED),
                DateTimeHelper.fromDb(r.get(EBD_ATTENDANCE.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_ATTENDANCE.UPDATED_AT)),
                r.get(EBD_ATTENDANCE.UPDATED_BY));
    }
}
