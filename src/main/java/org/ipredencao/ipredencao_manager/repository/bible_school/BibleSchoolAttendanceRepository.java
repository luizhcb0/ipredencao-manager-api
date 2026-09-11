package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolAttendanceRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolAttendance;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolAttendanceQuery;
import org.ipredencao.ipredencao_manager.repository.QueryConditions;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_ATTENDANCE;
import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_ENROLLMENT;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;

@Repository
public class BibleSchoolAttendanceRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolAttendance insert(BibleSchoolAttendance attendance) {
        Record rec = dsl.insertInto(BIBLE_SCHOOL_ATTENDANCE)
                .set(toRecord(attendance))
                .returning(BIBLE_SCHOOL_ATTENDANCE.ID)
                .fetchOne();
        return find(BibleSchoolAttendanceQuery.builder().id(rec.get(BIBLE_SCHOOL_ATTENDANCE.ID)).build()).getFirst();
    }

    public BibleSchoolAttendance update(BibleSchoolAttendance attendance) {
        dsl.update(BIBLE_SCHOOL_ATTENDANCE)
                .set(toRecord(attendance))
                .where(BIBLE_SCHOOL_ATTENDANCE.ID.eq(attendance.id()))
                .execute();
        return find(BibleSchoolAttendanceQuery.builder().id(attendance.id()).build()).getFirst();
    }

    public void delete(Long id) {
        dsl.deleteFrom(BIBLE_SCHOOL_ATTENDANCE).where(BIBLE_SCHOOL_ATTENDANCE.ID.eq(id)).execute();
    }

    public List<BibleSchoolAttendance> find(BibleSchoolAttendanceQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return baseSelect().where(where)
                .orderBy(PESSOA.NOME.asc(), BIBLE_SCHOOL_ATTENDANCE.ID.asc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(BibleSchoolAttendanceQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(BIBLE_SCHOOL_ATTENDANCE.ID.eq(query.id()));
        if (query.lessonId() != null) conditions.add(BIBLE_SCHOOL_ATTENDANCE.LESSON_ID.eq(query.lessonId()));
        if (query.enrollmentId() != null) {
            conditions.add(BIBLE_SCHOOL_ATTENDANCE.ENROLLMENT_ID.eq(query.enrollmentId()));
        }
        return conditions;
    }

    private BibleSchoolAttendanceRecord toRecord(BibleSchoolAttendance attendance) {
        BibleSchoolAttendanceRecord rec = new BibleSchoolAttendanceRecord();
        rec.setLessonId(attendance.lessonId());
        rec.setEnrollmentId(attendance.enrollmentId());
        rec.setPresent(attendance.present());
        rec.setUpdatedBy(attendance.updatedBy());
        return rec;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(BIBLE_SCHOOL_ATTENDANCE.asterisk(), BIBLE_SCHOOL_ENROLLMENT.PERSON_ID,
                        PESSOA.NOME.as("person_name"))
                .from(BIBLE_SCHOOL_ATTENDANCE)
                .join(BIBLE_SCHOOL_ENROLLMENT).on(BIBLE_SCHOOL_ENROLLMENT.ID.eq(BIBLE_SCHOOL_ATTENDANCE.ENROLLMENT_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(BIBLE_SCHOOL_ENROLLMENT.PERSON_ID));
    }

    private BibleSchoolAttendance fromRecord(Record r) {
        return new BibleSchoolAttendance(
                r.get(BIBLE_SCHOOL_ATTENDANCE.ID),
                r.get(BIBLE_SCHOOL_ATTENDANCE.LESSON_ID),
                r.get(BIBLE_SCHOOL_ATTENDANCE.ENROLLMENT_ID),
                r.get(BIBLE_SCHOOL_ENROLLMENT.PERSON_ID),
                r.get("person_name", String.class),
                r.get(BIBLE_SCHOOL_ATTENDANCE.PRESENT),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_ATTENDANCE.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_ATTENDANCE.UPDATED_AT)),
                r.get(BIBLE_SCHOOL_ATTENDANCE.UPDATED_BY));
    }
}
