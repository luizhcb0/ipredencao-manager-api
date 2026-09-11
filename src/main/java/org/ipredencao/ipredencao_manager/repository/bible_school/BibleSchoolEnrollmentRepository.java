package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.enums.BibleSchoolEnrollmentRole;
import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolEnrollmentRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollment;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentRoleEnum;
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

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_CLASS;
import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_ENROLLMENT;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;

@Repository
public class BibleSchoolEnrollmentRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolEnrollment insert(BibleSchoolEnrollment enrollment) {
        Record rec = dsl.insertInto(BIBLE_SCHOOL_ENROLLMENT)
                .set(toRecord(enrollment))
                .returning(BIBLE_SCHOOL_ENROLLMENT.ID)
                .fetchOne();
        return find(BibleSchoolEnrollmentQuery.builder().id(rec.get(BIBLE_SCHOOL_ENROLLMENT.ID)).build()).getFirst();
    }

    public void delete(Long id) {
        dsl.deleteFrom(BIBLE_SCHOOL_ENROLLMENT).where(BIBLE_SCHOOL_ENROLLMENT.ID.eq(id)).execute();
    }

    public List<BibleSchoolEnrollment> find(BibleSchoolEnrollmentQuery query) {
        return baseSelect()
                .where(QueryConditions.reduceToAnd(buildConditions(query)))
                .orderBy(BIBLE_SCHOOL_ENROLLMENT.ROLE.asc(), PESSOA.NOME.asc(), BIBLE_SCHOOL_ENROLLMENT.ID.asc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(BibleSchoolEnrollmentQuery q) {
        List<Condition> conditions = new ArrayList<>();
        if (q == null) return conditions;
        if (q.id() != null) conditions.add(BIBLE_SCHOOL_ENROLLMENT.ID.eq(q.id()));
        if (q.classId() != null) conditions.add(BIBLE_SCHOOL_ENROLLMENT.CLASS_ID.eq(q.classId()));
        if (q.personId() != null) conditions.add(BIBLE_SCHOOL_ENROLLMENT.PERSON_ID.eq(q.personId()));
        if (q.role() != null) {
            conditions.add(BIBLE_SCHOOL_ENROLLMENT.ROLE.eq(BibleSchoolEnrollmentRole.valueOf(q.role().name())));
        }
        return conditions;
    }

    private BibleSchoolEnrollmentRecord toRecord(BibleSchoolEnrollment enrollment) {
        BibleSchoolEnrollmentRecord rec = new BibleSchoolEnrollmentRecord();
        rec.setClassId(enrollment.classId());
        rec.setPersonId(enrollment.personId());
        rec.setRole(BibleSchoolEnrollmentRole.valueOf(enrollment.role().name()));
        rec.setUpdatedBy(enrollment.updatedBy());
        return rec;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(
                        BIBLE_SCHOOL_ENROLLMENT.asterisk(),
                        BIBLE_SCHOOL_CLASS.NAME.as("class_name"),
                        PESSOA.NOME.as("person_name"))
                .from(BIBLE_SCHOOL_ENROLLMENT)
                .join(BIBLE_SCHOOL_CLASS).on(BIBLE_SCHOOL_CLASS.ID.eq(BIBLE_SCHOOL_ENROLLMENT.CLASS_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(BIBLE_SCHOOL_ENROLLMENT.PERSON_ID));
    }

    private BibleSchoolEnrollment fromRecord(Record r) {
        BibleSchoolEnrollmentRole role = r.get(BIBLE_SCHOOL_ENROLLMENT.ROLE);
        return new BibleSchoolEnrollment(
                r.get(BIBLE_SCHOOL_ENROLLMENT.ID),
                r.get(BIBLE_SCHOOL_ENROLLMENT.CLASS_ID),
                r.get("class_name", String.class),
                r.get(BIBLE_SCHOOL_ENROLLMENT.PERSON_ID),
                r.get("person_name", String.class),
                role != null ? BibleSchoolEnrollmentRoleEnum.valueOf(role.name()) : null,
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_ENROLLMENT.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_ENROLLMENT.UPDATED_AT)),
                r.get(BIBLE_SCHOOL_ENROLLMENT.UPDATED_BY));
    }
}
