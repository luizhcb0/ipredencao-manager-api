package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.enums.BibleSchoolLessonStatus;
import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolLessonRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLesson;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonStatusEnum;
import org.ipredencao.ipredencao_manager.repository.QueryConditions;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_LESSON;

@Repository
public class BibleSchoolLessonRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolLesson insert(BibleSchoolLesson lesson) {
        Record rec = dsl.insertInto(BIBLE_SCHOOL_LESSON)
                .set(toRecord(lesson))
                .returning()
                .fetchOne();
        return fromRecord(rec);
    }

    public BibleSchoolLesson update(BibleSchoolLesson lesson) {
        dsl.update(BIBLE_SCHOOL_LESSON)
                .set(toRecord(lesson))
                .where(BIBLE_SCHOOL_LESSON.ID.eq(lesson.id()))
                .execute();
        return find(BibleSchoolLessonQuery.builder().id(lesson.id()).build()).getFirst();
    }

    public void delete(Long id) {
        dsl.deleteFrom(BIBLE_SCHOOL_LESSON).where(BIBLE_SCHOOL_LESSON.ID.eq(id)).execute();
    }

    public List<BibleSchoolLesson> find(BibleSchoolLessonQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.selectFrom(BIBLE_SCHOOL_LESSON)
                .where(where)
                .orderBy(BIBLE_SCHOOL_LESSON.LESSON_DATE.asc(), BIBLE_SCHOOL_LESSON.ID.asc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(BibleSchoolLessonQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(BIBLE_SCHOOL_LESSON.ID.eq(query.id()));
        if (query.classId() != null) conditions.add(BIBLE_SCHOOL_LESSON.CLASS_ID.eq(query.classId()));
        if (query.status() != null) {
            conditions.add(BIBLE_SCHOOL_LESSON.STATUS.eq(BibleSchoolLessonStatus.valueOf(query.status().name())));
        }
        return conditions;
    }

    private BibleSchoolLessonRecord toRecord(BibleSchoolLesson lesson) {
        BibleSchoolLessonRecord rec = new BibleSchoolLessonRecord();
        rec.setClassId(lesson.classId());
        rec.setTitle(lesson.title());
        rec.setDescription(lesson.description());
        rec.setLessonDate(DateTimeHelper.toDbDate(lesson.lessonDate()));
        rec.setStatus(BibleSchoolLessonStatus.valueOf(lesson.status().name()));
        rec.setUpdatedBy(lesson.updatedBy());
        return rec;
    }

    private BibleSchoolLesson fromRecord(Record r) {
        BibleSchoolLessonStatus status = r.get(BIBLE_SCHOOL_LESSON.STATUS);
        return new BibleSchoolLesson(
                r.get(BIBLE_SCHOOL_LESSON.ID),
                r.get(BIBLE_SCHOOL_LESSON.CLASS_ID),
                r.get(BIBLE_SCHOOL_LESSON.TITLE),
                r.get(BIBLE_SCHOOL_LESSON.DESCRIPTION),
                DateTimeHelper.fromDbDate(r.get(BIBLE_SCHOOL_LESSON.LESSON_DATE)),
                status != null ? BibleSchoolLessonStatusEnum.valueOf(status.name()) : null,
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_LESSON.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_LESSON.UPDATED_AT)),
                r.get(BIBLE_SCHOOL_LESSON.UPDATED_BY));
    }
}
