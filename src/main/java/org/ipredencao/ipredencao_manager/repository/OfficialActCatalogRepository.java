package org.ipredencao.ipredencao_manager.repository;

import jakarta.annotation.PostConstruct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActType;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_FORM;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_TYPE;

/** Cache em memória dos tipos e formas de atos oficiais, carregado uma vez no startup. */
@Repository
public class OfficialActCatalogRepository {

    @Autowired
    private DSLContext dsl;

    private List<OfficialActType> types;
    private Map<Long, OfficialActType> typesById;
    private Map<Long, OfficialActForm> formsById;

    @PostConstruct
    public void load() {
        Map<Long, OfficialActType> typeMap = new HashMap<>();
        Map<Long, OfficialActForm> formMap = new HashMap<>();

        for (Record r : dsl.select().from(OFFICIAL_ACT_TYPE).orderBy(OFFICIAL_ACT_TYPE.ID.asc()).fetch()) {
            OfficialActType t = new OfficialActType();
            t.setId(r.get(OFFICIAL_ACT_TYPE.ID));
            t.setName(r.get(OFFICIAL_ACT_TYPE.NAME));
            t.setCategory(r.get(OFFICIAL_ACT_TYPE.CATEGORY));
            t.setReferenceArticle(r.get(OFFICIAL_ACT_TYPE.REFERENCE_ARTICLE));
            t.setForms(new ArrayList<>());
            typeMap.put(t.getId(), t);
        }

        for (Record r : dsl.select().from(OFFICIAL_ACT_FORM).orderBy(OFFICIAL_ACT_FORM.ID.asc()).fetch()) {
            OfficialActForm f = new OfficialActForm();
            f.setId(r.get(OFFICIAL_ACT_FORM.ID));
            f.setOfficialActTypeId(r.get(OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID));
            f.setName(r.get(OFFICIAL_ACT_FORM.NAME));
            f.setArticleClause(r.get(OFFICIAL_ACT_FORM.ARTICLE_CLAUSE));
            f.setMetadataSchema(OfficialActFormEnum.fromId(f.getId()).getMetadataFields());

            formMap.put(f.getId(), f);

            OfficialActType parent = typeMap.get(f.getOfficialActTypeId());
            if (parent != null) {
                parent.getForms().add(f);
            }
        }

        List<OfficialActType> ordered = new ArrayList<>(typeMap.values());
        ordered.sort(Comparator.comparing(OfficialActType::getId));

        this.types = List.copyOf(ordered);
        this.typesById = Map.copyOf(typeMap);
        this.formsById = Map.copyOf(formMap);
    }

    public List<OfficialActType> getTypes() {
        return types;
    }

    public OfficialActType getTypeById(Long id) {
        OfficialActType t = typesById.get(id);
        if (t == null) {
            throw new IllegalArgumentException("Tipo de ato oficial não encontrado: " + id);
        }
        return t;
    }

    public OfficialActForm getFormById(Long id) {
        OfficialActForm f = formsById.get(id);
        if (f == null) {
            throw new IllegalArgumentException("Forma de ato oficial não encontrada: " + id);
        }
        return f;
    }

    public Optional<OfficialActForm> findFormById(Long id) {
        return Optional.ofNullable(formsById.get(id));
    }
}
