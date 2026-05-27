package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.FieldType;
import org.ipredencao.ipredencao_manager.model.official_act.MetadataFieldSpec;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActUpdateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.ipredencao.ipredencao_manager.repository.OfficialActTypesRepository;
import org.ipredencao.ipredencao_manager.repository.OfficialActRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class OfficialActService {

    @Autowired
    private OfficialActRepository repo;
    @Autowired
    private OfficialActTypesRepository officialActTypesRepository;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public List<OfficialAct> create(OfficialActCreateForm form) {
        validateCreateForm(form);
        validateMetadata(form.getOfficialActFormId(), form.getMetadata());

        Long currentUserId = securityUtils.getCurrentUserId();
        List<Long> personIds = new ArrayList<>(new LinkedHashSet<>(form.getPersonIds()));

        List<OfficialAct> created = new ArrayList<>();
        for (Long personId : personIds) {
            OfficialAct act = buildAct(form, personId, currentUserId);
            assignAdmissionOrderNumber(act, form);
            act = repo.insert(act);

            if (!form.isSkipEffects()) {
                persistCreateEffects(act);
            }
            created.add(act);
        }
        return created;
    }

    @Transactional
    public OfficialAct update(Long id, OfficialActUpdateForm form) {
        OfficialAct existing = repo.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Ato oficial " + id + " não encontrado");
        }
        if (form.getMetadata() != null) {
            validateMetadata(existing.getOfficialActFormId(), form.getMetadata());
            existing.setMetadata(form.getMetadata());
        }
        existing.setMinuteNumber(form.getMinuteNumber());
        existing.setMinuteDate(form.getMinuteDate());
        existing.setNotes(form.getNotes());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());
        return repo.update(existing);
    }

    @Transactional
    public void delete(Long id) {
        OfficialAct act = repo.findById(id);
        if (act == null) {
            throw new NoSuchElementException("Ato oficial " + id + " não encontrado");
        }

        Long previousCategoriaId = pessoaService
                .findCategoriaIdBefore(act.getPersonId(), act.getAddedAt())
                .orElse(null);

        Pessoa pessoa = pessoaService.findById(act.getPersonId());
        applyDeleteEffects(act, pessoa, previousCategoriaId);
        pessoaService.update(pessoa);

        repo.delete(id);
    }

    public OfficialAct findById(Long id) {
        OfficialAct act = repo.findById(id);
        if (act == null) {
            throw new NoSuchElementException("Ato oficial " + id + " não encontrado");
        }
        return act;
    }

    public List<OfficialAct> findByMinuteNumber(String minuteNumber) {
        return repo.findByMinuteNumber(minuteNumber);
    }

    public List<OfficialAct> findByPersonId(Long personId) {
        return repo.findByPersonId(personId);
    }

    public PagedResponse<OfficialAct> findPaginated(OfficialActQuery query) {
        if (query.getPagination() == null) query.setPagination(new PaginationParameters());
        query.getPagination().applyDefaults();

        List<OfficialAct> acts = repo.findByQuery(query);
        long total = repo.count(query);
        PageInfo page = new PageInfo(
                query.getPagination().getLimit(),
                query.getPagination().getOffset(),
                total);
        return new PagedResponse<>(acts, page);
    }

    private void validateCreateForm(OfficialActCreateForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Form de criação é obrigatório");
        }
        if (form.getOfficialActFormId() == null) {
            throw new IllegalArgumentException("officialActFormId é obrigatório");
        }
        officialActTypesRepository.getFormById(form.getOfficialActFormId());
        if (form.getActDate() == null) {
            throw new IllegalArgumentException("actDate é obrigatório");
        }
        if (form.getPersonIds() == null || form.getPersonIds().isEmpty()) {
            throw new IllegalArgumentException("personIds deve conter ao menos uma pessoa");
        }
        if (form.getPersonIds().stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("personIds não pode conter valores nulos");
        }
    }

    void validateMetadata(Long formId, Map<String, Object> metadata) {
        OfficialActForm form = officialActTypesRepository.getFormById(formId);
        Map<String, Object> safe = metadata != null ? metadata : Map.of();
        for (MetadataFieldSpec spec : form.getMetadataSchema()) {
            if (!spec.required()) continue;
            Object value = safe.get(spec.key());
            if (value == null) {
                throw new IllegalArgumentException(
                        "Campo obrigatório ausente em metadata: " + spec.key()
                                + " (" + spec.helperText() + ")");
            }
            if (spec.type() == FieldType.PERSON_REF) {
                if (!(value instanceof Map<?, ?> m)) {
                    throw new IllegalArgumentException(
                            "Campo obrigatório vazio em metadata: " + spec.key());
                }
                String name = stringOrNull(m.get("name"));
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException(
                            "Campo obrigatório vazio em metadata: " + spec.key());
                }
                if (m.containsKey("id") && !(m.get("id") instanceof Number)) {
                    throw new IllegalArgumentException(
                            "ID inválido em PERSON_REF: " + spec.key());
                }
            } else if (value instanceof String s && s.isBlank()) {
                throw new IllegalArgumentException(
                        "Campo obrigatório vazio em metadata: " + spec.key());
            }
        }
    }

    private static String stringOrNull(Object v) {
        return v != null ? v.toString() : null;
    }

    void assignAdmissionOrderNumber(OfficialAct act, OfficialActCreateForm form) {
        if (form.isSkipAdmissionOrderNumber()) return;
        OfficialActFormEnum formEnum = OfficialActFormEnum.fromId(act.getOfficialActFormId());
        if (formEnum.isAdmission()) {
            act.setAdmissionOrderNumber(repo.nextAdmissionOrderNumber());
        } else if (formEnum.isPromotionFromMnc()) {
            repo.findLatestAdmissionOrderNumber(act.getPersonId())
                    .ifPresent(act::setAdmissionOrderNumber);
        }
    }

    private void persistCreateEffects(OfficialAct act) {
        Pessoa pessoa = pessoaService.findById(act.getPersonId());
        applyCreateEffects(act, pessoa);
        pessoaService.update(pessoa);
    }

    private void applyCreateEffects(OfficialAct act, Pessoa pessoa) {
        OfficialActFormEnum form = OfficialActFormEnum.fromId(act.getOfficialActFormId());
        switch (form) {
            case ADM_MC_PROFISSAO_FE,
                 ADM_MC_CARTA_TRANSFERENCIA,
                 ADM_MC_JURISDICAO_A_PEDIDO,
                 ADM_MC_JURISDICAO_EX_OFFICIO,
                 ADM_MC_RESTAURACAO,
                 ADM_MC_DESIGNACAO_PRESBITERIO ->
                pessoa.setCategoria(CategoriaEnum.MEMBRO_COMUNGANTE);

            case ADM_MC_PROFISSAO_FE_E_BATISMO -> {
                pessoa.setCategoria(CategoriaEnum.MEMBRO_COMUNGANTE);
                pessoa.setDataBatismo(act.getActDate().toDateTimeAtStartOfDay());
                pessoa.setTipoBatismo(TipoBatismo.ADULTO);
                pessoa.setDataProfissaoDeFe(act.getActDate().toDateTimeAtStartOfDay());
            }

            case ADM_MNC_BATISMO_INFANCIA -> {
                pessoa.setCategoria(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
                pessoa.setDataBatismo(act.getActDate().toDateTimeAtStartOfDay());
                pessoa.setTipoBatismo(TipoBatismo.INFANTIL);
            }

            case ADM_MNC_TRANSFERENCIA_PAIS,
                 ADM_MNC_JURISDICAO_PAIS ->
                pessoa.setCategoria(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);

            case DEM_MC_EXCLUSAO_DISCIPLINA,
                 DEM_MC_EXCLUSAO_A_PEDIDO,
                 DEM_MC_EXCLUSAO_AUSENCIA,
                 DEM_MC_CARTA_TRANSFERENCIA,
                 DEM_MC_JURISDICAO_OUTRA_IGREJA,
                 DEM_MC_ORDENACAO_MINISTRO,
                 DEM_MNC_TRANSF_PAIS,
                 DEM_MNC_TRANSF_PROPRIA,
                 DEM_MNC_MAIORIDADE,
                 DEM_MNC_SOLIC_PAIS_OUTRA ->
                pessoa.setCategoria(CategoriaEnum.EX_MEMBRO);

            case DEM_MC_FALECIMENTO,
                 DEM_MNC_FALECIMENTO -> {
                pessoa.setCategoria(CategoriaEnum.EX_MEMBRO);
                pessoa.setDataFalecimento(act.getActDate().toDateTimeAtStartOfDay());
            }

            case DEM_MNC_PROFISSAO_FE -> {                          // Art. 24, d (promoção MNC -> MC)
                pessoa.setCategoria(CategoriaEnum.MEMBRO_COMUNGANTE);
                pessoa.setDataProfissaoDeFe(act.getActDate().toDateTimeAtStartOfDay());
            }
        }
    }

    // Reverte categoria; datas (batismo, profissão, falecimento) não são revertidas.
    private void applyDeleteEffects(OfficialAct act, Pessoa pessoa, Long previousCategoriaId) {
        if (previousCategoriaId == null) return;
        pessoa.setCategoria(CategoriaEnum.fromId(previousCategoriaId));
    }

    private OfficialAct buildAct(OfficialActCreateForm form, Long personId, Long currentUserId) {
        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(form.getOfficialActFormId());
        act.setPersonId(personId);
        act.setActDate(form.getActDate());
        act.setMinuteNumber(form.getMinuteNumber());
        act.setMinuteDate(form.getMinuteDate());
        act.setMetadata(form.getMetadata() != null ? form.getMetadata() : new HashMap<>());
        act.setNotes(form.getNotes());
        act.setUpdatedBy(currentUserId);
        return act;
    }
}
