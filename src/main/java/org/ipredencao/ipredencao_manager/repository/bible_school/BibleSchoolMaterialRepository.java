package org.ipredencao.ipredencao_manager.repository.bible_school;

import org.ipredencao.ipredencao_manager.jooq.tables.records.BibleSchoolMaterialRecord;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolMaterial;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolMaterialQuery;
import org.ipredencao.ipredencao_manager.repository.QueryConditions;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.ipredencao.ipredencao_manager.jooq.Tables.BIBLE_SCHOOL_MATERIAL;

@Repository
public class BibleSchoolMaterialRepository {

    @Autowired
    private DSLContext dsl;

    public BibleSchoolMaterial insert(BibleSchoolMaterial material) {
        Long id = dsl.insertInto(BIBLE_SCHOOL_MATERIAL)
                .set(toRecord(material))
                .returning(BIBLE_SCHOOL_MATERIAL.ID)
                .fetchOne(BIBLE_SCHOOL_MATERIAL.ID);
        return find(BibleSchoolMaterialQuery.builder().id(id).build()).getFirst();
    }

    public void delete(Long id) {
        dsl.deleteFrom(BIBLE_SCHOOL_MATERIAL).where(BIBLE_SCHOOL_MATERIAL.ID.eq(id)).execute();
    }

    public List<BibleSchoolMaterial> find(BibleSchoolMaterialQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.select(BIBLE_SCHOOL_MATERIAL.ID, BIBLE_SCHOOL_MATERIAL.CLASS_ID, BIBLE_SCHOOL_MATERIAL.LESSON_ID,
                        BIBLE_SCHOOL_MATERIAL.FILE_NAME, BIBLE_SCHOOL_MATERIAL.CONTENT_TYPE,
                        BIBLE_SCHOOL_MATERIAL.FILE_SIZE_BYTES, BIBLE_SCHOOL_MATERIAL.ADDED_AT,
                        BIBLE_SCHOOL_MATERIAL.UPDATED_BY)
                .from(BIBLE_SCHOOL_MATERIAL)
                .where(where)
                .orderBy(BIBLE_SCHOOL_MATERIAL.ADDED_AT.asc(), BIBLE_SCHOOL_MATERIAL.ID.asc())
                .fetch(r -> fromRecord(r, false));
    }

    public BibleSchoolMaterial findWithData(Long id) {
        Record r = dsl.selectFrom(BIBLE_SCHOOL_MATERIAL).where(BIBLE_SCHOOL_MATERIAL.ID.eq(id)).fetchOne();
        if (r == null) {
            throw new NoSuchElementException("Material " + id + " não encontrado");
        }
        return fromRecord(r, true);
    }

    private List<Condition> buildConditions(BibleSchoolMaterialQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(BIBLE_SCHOOL_MATERIAL.ID.eq(query.id()));
        if (query.classId() != null) conditions.add(BIBLE_SCHOOL_MATERIAL.CLASS_ID.eq(query.classId()));
        if (query.lessonId() != null) conditions.add(BIBLE_SCHOOL_MATERIAL.LESSON_ID.eq(query.lessonId()));
        if (Boolean.TRUE.equals(query.generalOnly())) conditions.add(BIBLE_SCHOOL_MATERIAL.LESSON_ID.isNull());
        return conditions;
    }

    private BibleSchoolMaterialRecord toRecord(BibleSchoolMaterial material) {
        BibleSchoolMaterialRecord rec = new BibleSchoolMaterialRecord();
        rec.setClassId(material.classId());
        rec.setLessonId(material.lessonId());
        rec.setFileName(material.fileName());
        rec.setContentType(material.contentType());
        rec.setFileSizeBytes(material.fileSizeBytes());
        rec.setFileData(material.data());
        rec.setUpdatedBy(material.updatedBy());
        return rec;
    }

    private BibleSchoolMaterial fromRecord(Record r, boolean withData) {
        return new BibleSchoolMaterial(
                r.get(BIBLE_SCHOOL_MATERIAL.ID),
                r.get(BIBLE_SCHOOL_MATERIAL.CLASS_ID),
                r.get(BIBLE_SCHOOL_MATERIAL.LESSON_ID),
                r.get(BIBLE_SCHOOL_MATERIAL.FILE_NAME),
                r.get(BIBLE_SCHOOL_MATERIAL.CONTENT_TYPE),
                r.get(BIBLE_SCHOOL_MATERIAL.FILE_SIZE_BYTES),
                withData ? r.get(BIBLE_SCHOOL_MATERIAL.FILE_DATA) : null,
                DateTimeHelper.fromDb(r.get(BIBLE_SCHOOL_MATERIAL.ADDED_AT)),
                r.get(BIBLE_SCHOOL_MATERIAL.UPDATED_BY));
    }
}
