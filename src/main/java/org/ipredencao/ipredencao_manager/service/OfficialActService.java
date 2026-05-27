package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.FieldType;
import org.ipredencao.ipredencao_manager.model.official_act.MetadataFieldSpec;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActCreateDto;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActUpdateDto;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.ipredencao.ipredencao_manager.repository.OfficialActCatalogRepository;
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
    private OfficialActCatalogRepository catalog;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private SecurityUtils securityUtils;

    // ===== CREATE =====

    @Transactional
    public List<OfficialAct> create(OfficialActCreateDto dto) {
        validateCreateDto(dto);
        validateMetadata(dto.getOfficialActFormId(), dto.getMetadata());

        Long currentUserId = securityUtils.getCurrentUserId();
        List<Long> personIds = new ArrayList<>(new LinkedHashSet<>(dto.getPersonIds()));

        List<OfficialAct> created = new ArrayList<>();
        for (Long personId : personIds) {
            OfficialAct act = buildAct(dto, personId, currentUserId);
            assignNumeroOrdemAdmissao(act, dto);
            act = repo.insert(act);

            if (!dto.isSkipEffects()) {
                persistCreateEffects(act);
            }
            created.add(act);
        }
        return created;
    }

    // ===== UPDATE (restritivo) =====

    @Transactional
    public OfficialAct update(Long id, OfficialActUpdateDto dto) {
        OfficialAct existing = repo.findById(id);
        if (existing == null) {
            throw new NoSuchElementException("Ato oficial " + id + " não encontrado");
        }
        // Metadata segue o mesmo contrato do create: campos required precisam estar
        // preenchidos. Validar antes de persistir para impedir downgrades acidentais.
        if (dto.getMetadata() != null) {
            validateMetadata(existing.getOfficialActFormId(), dto.getMetadata());
            existing.setMetadata(dto.getMetadata());
        }
        existing.setMinuteNumber(dto.getMinuteNumber());
        existing.setMinuteDate(dto.getMinuteDate());
        existing.setNotes(dto.getNotes());
        existing.setUpdatedBy(securityUtils.getCurrentUserId());
        return repo.update(existing);
    }

    // ===== DELETE (reverte categoria via histórico) =====

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

    // ===== READS =====

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

    // ===== VALIDAÇÃO =====

    private void validateCreateDto(OfficialActCreateDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO de criação é obrigatório");
        }
        if (dto.getOfficialActFormId() == null) {
            throw new IllegalArgumentException("officialActFormId é obrigatório");
        }
        catalog.getFormById(dto.getOfficialActFormId());
        if (dto.getActDate() == null) {
            throw new IllegalArgumentException("actDate é obrigatório");
        }
        if (dto.getPersonIds() == null || dto.getPersonIds().isEmpty()) {
            throw new IllegalArgumentException("personIds deve conter ao menos uma pessoa");
        }
        if (dto.getPersonIds().stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("personIds não pode conter valores nulos");
        }
    }

    void validateMetadata(Long formId, Map<String, Object> metadata) {
        OfficialActForm form = catalog.getFormById(formId);
        Map<String, Object> safe = metadata != null ? metadata : Map.of();
        for (MetadataFieldSpec spec : form.getMetadataSchema()) {
            if (!spec.required()) continue;
            Object value = safe.get(spec.key());
            if (value == null) {
                throw new IllegalArgumentException(
                        "Campo obrigatório ausente em metadata: " + spec.key()
                                + " (" + spec.helperText() + ")");
            }
            // PERSON_REF é armazenado como objeto {id?, name}; exige pelo menos `name` preenchido.
            // Demais tipos exigem string não-vazia (DATE também chega como ISO string).
            if (spec.type() == FieldType.PERSON_REF) {
                if (!(value instanceof Map<?, ?> m) || isBlank(stringOrNull(m.get("name")))) {
                    throw new IllegalArgumentException(
                            "Campo obrigatório vazio em metadata: " + spec.key());
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ===== NÚMERO DE ORDEM DE ADMISSÃO =====

    void assignNumeroOrdemAdmissao(OfficialAct act, OfficialActCreateDto dto) {
        if (dto.isSkipNumeroOrdemAdmissao()) return;
        OfficialActFormEnum form = OfficialActFormEnum.fromId(act.getOfficialActFormId());
        if (form.isAdmission()) {
            act.setNumeroOrdemAdmissao(repo.nextNumeroOrdemAdmissao());
        } else if (form.isPromotionFromMnc()) {
            repo.findLatestNumeroOrdemAdmissao(act.getPersonId())
                    .ifPresent(act::setNumeroOrdemAdmissao);
            // Se a pessoa não tinha admissão anterior (backfill incompleto), fica null.
        }
    }

    // ===== EFEITOS NA PESSOA (regras de domínio) =====

    /** Carrega a pessoa, aplica as mudanças do ato em memória e persiste. */
    private void persistCreateEffects(OfficialAct act) {
        Pessoa pessoa = pessoaService.findById(act.getPersonId());
        applyCreateEffects(act, pessoa);
        pessoaService.update(pessoa);
    }

    /** Muta {@code pessoa} aplicando categoria/datas implicadas pela forma do ato. */
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
                pessoa.setDataBatismo(act.getActDate());
                pessoa.setTipoBatismo(TipoBatismo.ADULTO);
                pessoa.setDataProfissaoDeFe(act.getActDate());
            }

            case ADM_MNC_BATISMO_INFANCIA -> {
                pessoa.setCategoria(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
                pessoa.setDataBatismo(act.getActDate());
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
                pessoa.setDataFalecimento(act.getActDate());
            }

            case DEM_MNC_PROFISSAO_FE -> {                          // Art. 24, d (promoção MNC -> MC)
                pessoa.setCategoria(CategoriaEnum.MEMBRO_COMUNGANTE);
                pessoa.setDataProfissaoDeFe(act.getActDate());
            }
        }
    }

    /**
     * Reverte apenas {@code categoria} para o valor anterior ao ato. Datas (batismo,
     * profissão, falecimento) NÃO são revertidas — têm valor histórico independente.
     */
    private void applyDeleteEffects(OfficialAct act, Pessoa pessoa, Long previousCategoriaId) {
        if (previousCategoriaId == null) return;
        pessoa.setCategoria(CategoriaEnum.fromId(previousCategoriaId));
    }

    private OfficialAct buildAct(OfficialActCreateDto dto, Long personId, Long currentUserId) {
        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(dto.getOfficialActFormId());
        act.setPersonId(personId);
        act.setActDate(dto.getActDate());
        act.setMinuteNumber(dto.getMinuteNumber());
        act.setMinuteDate(dto.getMinuteDate());
        act.setMetadata(dto.getMetadata() != null ? dto.getMetadata() : new HashMap<>());
        act.setNotes(dto.getNotes());
        act.setUpdatedBy(currentUserId);
        return act;
    }
}
