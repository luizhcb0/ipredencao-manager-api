package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.enums.EbdClassStatus;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassSyllabus;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CLASS;
import static org.ipredencao.ipredencao_manager.jooq.Tables.EBD_CYCLE;

@Repository
public class EbdClassRepository {

    @Autowired
    private DSLContext dsl;

    public EbdClass insert(Long cycleId, String name, String description,
                           EbdClassStatusEnum status, Long updatedBy) {
        Record rec = dsl.insertInto(EBD_CLASS)
                .set(writableColumns(cycleId, name, description, status, updatedBy))
                .returning(EBD_CLASS.ID)
                .fetchOne();
        return find(EbdClassQuery.builder().id(rec.get(EBD_CLASS.ID)).build()).getFirst();
    }

    public void update(Long id, Long cycleId, String name, String description,
                       EbdClassStatusEnum status, Long updatedBy) {
        dsl.update(EBD_CLASS)
                .set(writableColumns(cycleId, name, description, status, updatedBy))
                .where(EBD_CLASS.ID.eq(id))
                .execute();
    }

    public List<EbdClass> find(EbdClassQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        PaginationParameters pagination = query != null ? query.pagination() : null;
        SelectConditionStep<Record> step = baseSelect().where(where);
        if (pagination != null) {
            pagination.applyDefaults();
            return step.orderBy(EBD_CLASS.NAME.asc(), EBD_CLASS.ID.asc())
                    .limit(pagination.getLimit())
                    .offset(pagination.getOffset())
                    .fetch(this::fromRecord);
        }
        return step.orderBy(EBD_CLASS.NAME.asc(), EBD_CLASS.ID.asc()).fetch(this::fromRecord);
    }

    public int count(EbdClassQuery query) {
        Condition where = QueryConditions.reduceToAnd(buildConditions(query));
        Integer total = dsl.selectCount().from(EBD_CLASS).where(where).fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    // Substitui a ementa inteira (nunca há mais de um arquivo por turma).
    public void updateSyllabus(Long id, String fileName, String contentType, long fileSizeBytes, byte[] fileData) {
        dsl.update(EBD_CLASS)
                .set(EBD_CLASS.SYLLABUS_FILE_NAME, fileName)
                .set(EBD_CLASS.SYLLABUS_CONTENT_TYPE, contentType)
                .set(EBD_CLASS.SYLLABUS_FILE_SIZE_BYTES, fileSizeBytes)
                .set(EBD_CLASS.SYLLABUS_FILE_DATA, fileData)
                .where(EBD_CLASS.ID.eq(id))
                .execute();
    }

    public void clearSyllabus(Long id) {
        dsl.update(EBD_CLASS)
                .setNull(EBD_CLASS.SYLLABUS_FILE_NAME)
                .setNull(EBD_CLASS.SYLLABUS_CONTENT_TYPE)
                .setNull(EBD_CLASS.SYLLABUS_FILE_SIZE_BYTES)
                .setNull(EBD_CLASS.SYLLABUS_FILE_DATA)
                .where(EBD_CLASS.ID.eq(id))
                .execute();
    }

    // Único ponto do repositório que carrega o BYTEA da ementa — só o download
    // passa por aqui. 404 tanto se a turma não existe quanto se ela existe mas
    // não tem ementa enviada (mesmo tratamento — ver EbdService.downloadSyllabus).
    public EbdClassSyllabus findSyllabusContent(Long id) {
        Record r = dsl.select(EBD_CLASS.SYLLABUS_FILE_NAME, EBD_CLASS.SYLLABUS_CONTENT_TYPE, EBD_CLASS.SYLLABUS_FILE_DATA)
                .from(EBD_CLASS)
                .where(EBD_CLASS.ID.eq(id))
                .fetchOne();
        if (r == null || r.get(EBD_CLASS.SYLLABUS_FILE_DATA) == null) {
            throw new NoSuchElementException("Ementa não encontrada");
        }
        return new EbdClassSyllabus(
                r.get(EBD_CLASS.SYLLABUS_FILE_NAME),
                r.get(EBD_CLASS.SYLLABUS_CONTENT_TYPE),
                r.get(EBD_CLASS.SYLLABUS_FILE_DATA));
    }

    private List<Condition> buildConditions(EbdClassQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;
        if (query.id() != null) conditions.add(EBD_CLASS.ID.eq(query.id()));
        QueryConditions.addUnaccentedLike(conditions, EBD_CLASS.NAME, query.name());
        if (query.cycleId() != null) conditions.add(EBD_CLASS.CYCLE_ID.eq(query.cycleId()));
        if (query.status() != null) conditions.add(EBD_CLASS.STATUS.eq(EbdClassStatus.valueOf(query.status().name())));
        return conditions;
    }

    private Map<Field<?>, Object> writableColumns(Long cycleId, String name, String description,
                                                   EbdClassStatusEnum status, Long updatedBy) {
        Map<Field<?>, Object> columns = new LinkedHashMap<>();
        columns.put(EBD_CLASS.CYCLE_ID, cycleId);
        columns.put(EBD_CLASS.NAME, name);
        columns.put(EBD_CLASS.DESCRIPTION, description);
        columns.put(EBD_CLASS.STATUS, EbdClassStatus.valueOf(status.name()));
        columns.put(EBD_CLASS.UPDATED_BY, updatedBy);
        return columns;
    }

    // Nunca seleciona syllabus_file_data (ver comentário na V015) — só
    // findSyllabusContent carrega o BYTEA. Field<?>[] (em vez dos fields soltos
    // como varargs) força o overload não tipado de select(...): com os fields
    // soltos o javac resolve pra Record12<...>, que não bate com o
    // SelectJoinStep<Record> desta assinatura.
    private SelectJoinStep<Record> baseSelect() {
        Field<?>[] fields = {
                EBD_CLASS.ID, EBD_CLASS.CYCLE_ID, EBD_CLASS.NAME, EBD_CLASS.DESCRIPTION,
                EBD_CLASS.SYLLABUS_FILE_NAME, EBD_CLASS.SYLLABUS_CONTENT_TYPE, EBD_CLASS.SYLLABUS_FILE_SIZE_BYTES,
                EBD_CLASS.STATUS, EBD_CLASS.ADDED_AT, EBD_CLASS.UPDATED_AT, EBD_CLASS.UPDATED_BY,
                EBD_CYCLE.NAME.as("cycle_name")
        };
        return dsl.select(fields)
                .from(EBD_CLASS)
                .leftJoin(EBD_CYCLE).on(EBD_CYCLE.ID.eq(EBD_CLASS.CYCLE_ID));
    }

    private EbdClass fromRecord(Record r) {
        EbdClassStatus status = r.get(EBD_CLASS.STATUS);
        return new EbdClass(
                r.get(EBD_CLASS.ID),
                r.get(EBD_CLASS.CYCLE_ID),
                r.get("cycle_name", String.class),
                r.get(EBD_CLASS.NAME),
                r.get(EBD_CLASS.DESCRIPTION),
                r.get(EBD_CLASS.SYLLABUS_FILE_NAME),
                r.get(EBD_CLASS.SYLLABUS_CONTENT_TYPE),
                r.get(EBD_CLASS.SYLLABUS_FILE_SIZE_BYTES),
                status != null ? EbdClassStatusEnum.valueOf(status.name()) : null,
                DateTimeHelper.fromDb(r.get(EBD_CLASS.ADDED_AT)),
                DateTimeHelper.fromDb(r.get(EBD_CLASS.UPDATED_AT)),
                r.get(EBD_CLASS.UPDATED_BY),
                null);
    }
}
