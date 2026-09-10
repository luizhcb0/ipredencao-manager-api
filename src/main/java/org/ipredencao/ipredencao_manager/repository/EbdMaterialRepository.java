package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterial;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterialContent;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterialQuery;
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
import java.util.NoSuchElementException;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_MATERIAL;

@Repository
public class EbdMaterialRepository {

    @Autowired
    private DSLContext dsl;

    public Long insert(Long classId, Long lessonId, String fileName, String contentType, long fileSizeBytes,
                       byte[] fileData, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_MATERIAL.CLASS_ID, classId);
        columns.put(EBD_MATERIAL.LESSON_ID, lessonId);
        columns.put(EBD_MATERIAL.FILE_NAME, fileName);
        columns.put(EBD_MATERIAL.CONTENT_TYPE, contentType);
        columns.put(EBD_MATERIAL.FILE_SIZE_BYTES, fileSizeBytes);
        columns.put(EBD_MATERIAL.FILE_DATA, fileData);
        columns.put(EBD_MATERIAL.UPDATED_BY, updatedBy);
        return dsl.insertInto(EBD_MATERIAL).set(columns).returning(EBD_MATERIAL.ID).fetchOne(EBD_MATERIAL.ID);
    }

    public void delete(Long id) {
        dsl.deleteFrom(EBD_MATERIAL).where(EBD_MATERIAL.ID.eq(id)).execute();
    }

    // Listagem: nunca seleciona file_data (ver comentário em V016).
    public List<EbdMaterial> find(EbdMaterialQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        return dsl.select(EBD_MATERIAL.ID, EBD_MATERIAL.CLASS_ID, EBD_MATERIAL.LESSON_ID, EBD_MATERIAL.FILE_NAME,
                        EBD_MATERIAL.CONTENT_TYPE, EBD_MATERIAL.FILE_SIZE_BYTES, EBD_MATERIAL.ADDED_AT,
                        EBD_MATERIAL.UPDATED_BY)
                .from(EBD_MATERIAL)
                .where(where)
                .orderBy(EBD_MATERIAL.ADDED_AT.asc(), EBD_MATERIAL.ID.asc())
                .fetch(this::fromMetadataRecord);
    }

    // Único ponto do repositório que carrega o BYTEA — só o download passa por aqui.
    public EbdMaterialContent findContent(Long id) {
        Record r = dsl.select(EBD_MATERIAL.CLASS_ID, EBD_MATERIAL.LESSON_ID, EBD_MATERIAL.FILE_NAME,
                        EBD_MATERIAL.CONTENT_TYPE, EBD_MATERIAL.FILE_DATA)
                .from(EBD_MATERIAL)
                .where(EBD_MATERIAL.ID.eq(id))
                .fetchOne();
        if (r == null) {
            throw new NoSuchElementException("Material " + id + " não encontrado");
        }
        return new EbdMaterialContent(
                r.get(EBD_MATERIAL.CLASS_ID),
                r.get(EBD_MATERIAL.LESSON_ID),
                r.get(EBD_MATERIAL.FILE_NAME),
                r.get(EBD_MATERIAL.CONTENT_TYPE),
                r.get(EBD_MATERIAL.FILE_DATA));
    }

    private List<Condition> buildConditions(EbdMaterialQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_MATERIAL.ID.eq(query.id()));
        if (query.classId() != null) conditions.add(EBD_MATERIAL.CLASS_ID.eq(query.classId()));
        if (query.lessonId() != null) conditions.add(EBD_MATERIAL.LESSON_ID.eq(query.lessonId()));
        if (Boolean.TRUE.equals(query.generalOnly())) conditions.add(EBD_MATERIAL.LESSON_ID.isNull());
        return conditions;
    }

    private EbdMaterial fromMetadataRecord(Record r) {
        return new EbdMaterial(
                r.get(EBD_MATERIAL.ID),
                r.get(EBD_MATERIAL.CLASS_ID),
                r.get(EBD_MATERIAL.LESSON_ID),
                r.get(EBD_MATERIAL.FILE_NAME),
                r.get(EBD_MATERIAL.CONTENT_TYPE),
                r.get(EBD_MATERIAL.FILE_SIZE_BYTES),
                DateTimeHelper.fromDb(r.get(EBD_MATERIAL.ADDED_AT)),
                r.get(EBD_MATERIAL.UPDATED_BY));
    }
}
