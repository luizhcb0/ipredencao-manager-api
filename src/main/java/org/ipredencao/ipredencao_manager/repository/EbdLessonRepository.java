package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.EbdLessonStatus;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLesson;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonStatusEnum;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_LESSON;

@Repository
public class EbdLessonRepository {

    @Autowired
    private DSLContext dsl;

    public Long insert(Long classId, String title, String description, String content, LocalDate lessonDate,
                       int displayOrder, EbdLessonStatusEnum status, Long updatedBy) {
        return dsl.insertInto(EBD_LESSON)
                .set(writableColumns(classId, title, description, content, lessonDate, displayOrder, status, updatedBy))
                .returning(EBD_LESSON.ID)
                .fetchOne(EBD_LESSON.ID);
    }

    public void update(Long id, Long classId, String title, String description, String content, LocalDate lessonDate,
                       int displayOrder, EbdLessonStatusEnum status, Long updatedBy) {
        dsl.update(EBD_LESSON)
                .set(writableColumns(classId, title, description, content, lessonDate, displayOrder, status, updatedBy))
                .where(EBD_LESSON.ID.eq(id))
                .execute();
    }

    public void delete(Long id) {
        dsl.deleteFrom(EBD_LESSON).where(EBD_LESSON.ID.eq(id)).execute();
    }

    public int countByClass(Long classId) {
        Integer total = dsl.selectCount().from(EBD_LESSON).where(EBD_LESSON.CLASS_ID.eq(classId)).fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    public List<EbdLesson> find(EbdLessonQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.selectFrom(EBD_LESSON)
                .where(where)
                .orderBy(EBD_LESSON.DISPLAY_ORDER.asc(), EBD_LESSON.ID.asc())
                .fetch(this::fromRecord);
    }

    private List<Condition> buildConditions(EbdLessonQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_LESSON.ID.eq(query.id()));
        if (query.classId() != null) conditions.add(EBD_LESSON.CLASS_ID.eq(query.classId()));
        if (query.status() != null) conditions.add(EBD_LESSON.STATUS.eq(EbdLessonStatus.valueOf(query.status().name())));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long classId, String title, String description, String content,
                                                   LocalDate lessonDate, int displayOrder, EbdLessonStatusEnum status,
                                                   Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_LESSON.CLASS_ID, classId);
        columns.put(EBD_LESSON.TITLE, title);
        columns.put(EBD_LESSON.DESCRIPTION, description);
        columns.put(EBD_LESSON.CONTENT, content);
        columns.put(EBD_LESSON.LESSON_DATE, DateTimeHelper.toDbDate(lessonDate));
        columns.put(EBD_LESSON.DISPLAY_ORDER, displayOrder);
        columns.put(EBD_LESSON.STATUS, EbdLessonStatus.valueOf(status.name()));
        columns.put(EBD_LESSON.UPDATED_BY, updatedBy);
        return columns;
    }

    private EbdLesson fromRecord(Record r) {
        EbdLessonStatus status = r.get(EBD_LESSON.STATUS);
        return new EbdLesson(
                r.get(EBD_LESSON.ID),
                r.get(EBD_LESSON.CLASS_ID),
                r.get(EBD_LESSON.TITLE),
                r.get(EBD_LESSON.DESCRIPTION),
                r.get(EBD_LESSON.CONTENT),
                DateTimeHelper.fromDbDate(r.get(EBD_LESSON.LESSON_DATE)),
                r.get(EBD_LESSON.DISPLAY_ORDER),
                status != null ? EbdLessonStatusEnum.valueOf(status.name()) : null,
                DateTimeHelper.fromDb(r.get(EBD_LESSON.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_LESSON.UPDATED_AT)),
                r.get(EBD_LESSON.UPDATED_BY));
    }
}
