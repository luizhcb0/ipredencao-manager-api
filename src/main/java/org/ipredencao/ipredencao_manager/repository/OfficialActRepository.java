package org.ipredencao.ipredencao_manager.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ipredencao.ipredencao_manager.jooq.Tables.CATEGORIA;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_FORM;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_TYPE;
import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA;

@Repository
public class OfficialActRepository {

    private static final String NUMERO_ORDEM_SEQ = "official_act_numero_ordem_admissao_seq";
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {};

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ObjectMapper objectMapper;

    public OfficialAct insert(OfficialAct act) {
        Long id = dsl.insertInto(OFFICIAL_ACT)
                .set(OFFICIAL_ACT.OFFICIAL_ACT_FORM_ID, act.getOfficialActFormId())
                .set(OFFICIAL_ACT.PERSON_ID, act.getPersonId())
                .set(OFFICIAL_ACT.ACT_DATE, DateTimeHelper.toDbDate(act.getActDate()))
                .set(OFFICIAL_ACT.MINUTE_NUMBER, act.getMinuteNumber())
                .set(OFFICIAL_ACT.MINUTE_DATE, DateTimeHelper.toDbDate(act.getMinuteDate()))
                .set(OFFICIAL_ACT.NUMERO_ORDEM_ADMISSAO, act.getNumeroOrdemAdmissao())
                .set(OFFICIAL_ACT.METADATA, toJsonb(act.getMetadata()))
                .set(OFFICIAL_ACT.NOTES, act.getNotes())
                .set(OFFICIAL_ACT.UPDATED_BY, act.getUpdatedBy())
                .returning(OFFICIAL_ACT.ID)
                .fetchOne(OFFICIAL_ACT.ID);
        return findById(id);
    }

    public OfficialAct update(OfficialAct act) {
        dsl.update(OFFICIAL_ACT)
                .set(OFFICIAL_ACT.MINUTE_NUMBER, act.getMinuteNumber())
                .set(OFFICIAL_ACT.MINUTE_DATE, DateTimeHelper.toDbDate(act.getMinuteDate()))
                .set(OFFICIAL_ACT.NOTES, act.getNotes())
                .set(OFFICIAL_ACT.METADATA, toJsonb(act.getMetadata()))
                .set(OFFICIAL_ACT.UPDATED_BY, act.getUpdatedBy())
                .where(OFFICIAL_ACT.ID.eq(act.getId()))
                .execute();
        return findById(act.getId());
    }

    public void delete(Long id) {
        dsl.deleteFrom(OFFICIAL_ACT)
                .where(OFFICIAL_ACT.ID.eq(id))
                .execute();
    }

    public OfficialAct findById(Long id) {
        if (id == null) return null;
        Record record = baseSelect()
                .where(OFFICIAL_ACT.ID.eq(id))
                .fetchOne();
        return record != null ? fromRecord(record) : null;
    }

    public List<OfficialAct> findByMinuteNumber(String minuteNumber) {
        if (minuteNumber == null || minuteNumber.isBlank()) return List.of();
        return baseSelect()
                .where(OFFICIAL_ACT.MINUTE_NUMBER.eq(minuteNumber))
                .orderBy(OFFICIAL_ACT_TYPE.ID.asc(), OFFICIAL_ACT_FORM.ID.asc(), OFFICIAL_ACT.ACT_DATE.asc(), OFFICIAL_ACT.ID.asc())
                .fetch(this::fromRecord);
    }

    public List<OfficialAct> findByPersonId(Long personId) {
        if (personId == null) return List.of();
        return baseSelect()
                .where(OFFICIAL_ACT.PERSON_ID.eq(personId))
                .orderBy(OFFICIAL_ACT.ACT_DATE.desc(), OFFICIAL_ACT.ID.desc())
                .fetch(this::fromRecord);
    }

    public List<OfficialAct> findByQuery(OfficialActQuery query) {
        List<Condition> conditions = buildConditions(query);
        Condition where = conditions.isEmpty() ? DSL.noCondition() : DSL.and(conditions);

        PaginationParameters pagination = query.getPagination();
        if (pagination != null) pagination.applyDefaults();

        SelectConditionStep<Record> step = baseSelect().where(where);
        if (pagination != null) {
            return step
                    .orderBy(OFFICIAL_ACT.ACT_DATE.desc(), OFFICIAL_ACT.ID.desc())
                    .limit(pagination.getLimit())
                    .offset(pagination.getOffset())
                    .fetch(this::fromRecord);
        }
        return step
                .orderBy(OFFICIAL_ACT.ACT_DATE.desc(), OFFICIAL_ACT.ID.desc())
                .fetch(this::fromRecord);
    }

    public int count(OfficialActQuery query) {
        List<Condition> conditions = buildConditions(query);
        Condition where = conditions.isEmpty() ? DSL.noCondition() : DSL.and(conditions);
        Integer total = dsl.selectCount()
                .from(OFFICIAL_ACT)
                .join(OFFICIAL_ACT_FORM).on(OFFICIAL_ACT_FORM.ID.eq(OFFICIAL_ACT.OFFICIAL_ACT_FORM_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(OFFICIAL_ACT.PERSON_ID))
                .where(where)
                .fetchOne(0, Integer.class);
        return total != null ? total : 0;
    }

    public Long nextNumeroOrdemAdmissao() {
        return dsl.fetchValue(DSL.field("nextval('" + NUMERO_ORDEM_SEQ + "')", Long.class));
    }

    /**
     * Retorna o número de ordem de admissão da admissão (ou promoção MNC→MC) mais recente da pessoa.
     * Usado pela promoção {@code DEM_MNC_PROFISSAO_FE} (Art. 24, d), que herda o número.
     */
    public Optional<Long> findLatestNumeroOrdemAdmissao(Long personId) {
        if (personId == null) return Optional.empty();
        Long value = dsl.select(OFFICIAL_ACT.NUMERO_ORDEM_ADMISSAO)
                .from(OFFICIAL_ACT)
                .where(OFFICIAL_ACT.PERSON_ID.eq(personId))
                .and(OFFICIAL_ACT.NUMERO_ORDEM_ADMISSAO.isNotNull())
                .orderBy(OFFICIAL_ACT.ACT_DATE.desc(), OFFICIAL_ACT.ID.desc())
                .limit(1)
                .fetchOne(OFFICIAL_ACT.NUMERO_ORDEM_ADMISSAO);
        return Optional.ofNullable(value);
    }

    private List<Condition> buildConditions(OfficialActQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (query == null) return conditions;

        if (query.getMinuteNumber() != null && !query.getMinuteNumber().isBlank()) {
            conditions.add(OFFICIAL_ACT.MINUTE_NUMBER.eq(query.getMinuteNumber()));
        }
        if (query.getStartDate() != null) {
            conditions.add(OFFICIAL_ACT.ACT_DATE.greaterOrEqual(DateTimeHelper.toDbDate(query.getStartDate())));
        }
        if (query.getEndDate() != null) {
            conditions.add(OFFICIAL_ACT.ACT_DATE.lessOrEqual(DateTimeHelper.toDbDate(query.getEndDate())));
        }
        if (query.getFormIds() != null && !query.getFormIds().isEmpty()) {
            conditions.add(OFFICIAL_ACT.OFFICIAL_ACT_FORM_ID.in(query.getFormIds()));
        }
        if (query.getTypeIds() != null && !query.getTypeIds().isEmpty()) {
            conditions.add(OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID.in(query.getTypeIds()));
        }
        if (query.getPersonId() != null) {
            conditions.add(OFFICIAL_ACT.PERSON_ID.eq(query.getPersonId()));
        }
        QueryConditions.addUnaccentedLike(conditions, PESSOA.NOME, query.getPersonName());
        return conditions;
    }

    private SelectJoinStep<Record> baseSelect() {
        return dsl.select(
                        OFFICIAL_ACT.asterisk(),
                        OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID,
                        OFFICIAL_ACT_FORM.NAME.as("form_name"),
                        OFFICIAL_ACT_FORM.ARTICLE_CLAUSE.as("article_clause"),
                        OFFICIAL_ACT_TYPE.NAME.as("type_name"),
                        OFFICIAL_ACT_TYPE.CATEGORY,
                        OFFICIAL_ACT_TYPE.REFERENCE_ARTICLE,
                        PESSOA.NOME.as("person_name"),
                        PESSOA.CATEGORIA_ID.as("current_categoria_id"),
                        CATEGORIA.NOME.as("current_categoria_nome"))
                .from(OFFICIAL_ACT)
                .join(OFFICIAL_ACT_FORM).on(OFFICIAL_ACT_FORM.ID.eq(OFFICIAL_ACT.OFFICIAL_ACT_FORM_ID))
                .join(OFFICIAL_ACT_TYPE).on(OFFICIAL_ACT_TYPE.ID.eq(OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID))
                .join(PESSOA).on(PESSOA.PESSOA_ID.eq(OFFICIAL_ACT.PERSON_ID))
                .leftJoin(CATEGORIA).on(CATEGORIA.ID.eq(PESSOA.CATEGORIA_ID));
    }

    private OfficialAct fromRecord(Record record) {
        OfficialAct act = new OfficialAct();
        act.setId(record.get(OFFICIAL_ACT.ID));
        act.setOfficialActFormId(record.get(OFFICIAL_ACT.OFFICIAL_ACT_FORM_ID));
        act.setPersonId(record.get(OFFICIAL_ACT.PERSON_ID));
        act.setActDate(DateTimeHelper.fromDbDate(record.get(OFFICIAL_ACT.ACT_DATE)));
        act.setMinuteNumber(record.get(OFFICIAL_ACT.MINUTE_NUMBER));
        act.setMinuteDate(DateTimeHelper.fromDbDate(record.get(OFFICIAL_ACT.MINUTE_DATE)));
        act.setNumeroOrdemAdmissao(record.get(OFFICIAL_ACT.NUMERO_ORDEM_ADMISSAO));
        act.setMetadata(fromJsonb(record.get(OFFICIAL_ACT.METADATA)));
        act.setNotes(record.get(OFFICIAL_ACT.NOTES));
        act.setAddedAt(DateTimeHelper.fromDb(record.get(OFFICIAL_ACT.ADDED_AT)));
        act.setUpdatedAt(DateTimeHelper.fromDb(record.get(OFFICIAL_ACT.UPDATED_AT)));
        act.setUpdatedBy(record.get(OFFICIAL_ACT.UPDATED_BY));

        act.setOfficialActTypeId(record.get(OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID));
        act.setFormName(record.get("form_name", String.class));
        act.setArticleClause(record.get("article_clause", String.class));
        act.setTypeName(record.get("type_name", String.class));
        act.setCategory(record.get(OFFICIAL_ACT_TYPE.CATEGORY));
        act.setReferenceArticle(record.get(OFFICIAL_ACT_TYPE.REFERENCE_ARTICLE));
        act.setPersonName(record.get("person_name", String.class));
        act.setCurrentCategoryId(record.get("current_categoria_id", Long.class));
        act.setCurrentCategoryName(record.get("current_categoria_nome", String.class));
        return act;
    }

    private JSONB toJsonb(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata != null ? metadata : Collections.emptyMap();
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(safe));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Falha ao serializar metadata do ato oficial", e);
        }
    }

    private Map<String, Object> fromJsonb(JSONB jsonb) {
        if (jsonb == null) return new HashMap<>();
        String data = jsonb.data();
        if (data == null || data.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(data, METADATA_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar metadata do ato oficial", e);
        }
    }
}
