package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.form.PregnancyCreateForm;
import org.ipredencao.ipredencao_manager.controller.form.PregnancyUpdateForm;
import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.ConfidentialAccess;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaInclude;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.pregnancy.PregnancyNaming;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.ipredencao.ipredencao_manager.repository.OfficialActRepository;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.joda.time.DateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PregnancyService {

    private static final long DEFAULT_BIRTH_CATEGORY_ID = CategoriaEnum.AGUARDANDO_BATISMO_INFANTIL.getId();

    private final PessoaService pessoaService;
    private final PessoaRepository pessoaRepository;
    private final OfficialActRepository officialActRepository;

    public PregnancyService(PessoaService pessoaService,
                              PessoaRepository pessoaRepository,
                              OfficialActRepository officialActRepository) {
        this.pessoaService = pessoaService;
        this.pessoaRepository = pessoaRepository;
        this.officialActRepository = officialActRepository;
    }

    @Transactional
    public Pessoa create(PregnancyCreateForm form) {
        if (form.getMotherId() == null) {
            throw new IllegalArgumentException("Mãe é obrigatória");
        }
        if (form.getFatherId() == null) {
            throw new IllegalArgumentException("Pai é obrigatório");
        }
        if (form.getExpectedDueDate() == null) {
            throw new IllegalArgumentException("DPP é obrigatória");
        }
        validateDueDateNotPast(form.getExpectedDueDate());
        if (form.isConfidential()) requireConfidentialAccess();

        Pessoa mother = pessoaService.findById(form.getMotherId());
        Pessoa father = resolveFather(form.getFatherId(), form.getMotherId());

        String nickname = PregnancyNaming.buildNickname(mother, father);
        String builtName = PregnancyNaming.buildName(form.getName(), nickname);

        Pessoa baby = new Pessoa();
        baby.setNome(builtName);
        baby.setApelido(nickname);
        baby.setCampus(mother.getCampus());
        baby.setCategoria(form.isConfidential() ? CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO : CategoriaEnum.GESTACAO);
        baby.setDataNascimento(form.getExpectedDueDate());
        baby.setSexo(form.getGender());
        baby.setChefeDeFamiliaId(resolveFamilyHead(mother));
        applyFamilyHeadAddress(baby);

        baby = pessoaService.create(baby);
        createParentRelationships(baby.getId(), mother.getId(), father.getId());

        return reloadWithRelationships(baby.getId());
    }

    @Transactional
    public Pessoa update(Long id, PregnancyUpdateForm form) {
        Pessoa pregnancy = loadPregnancy(id);

        if (form.getBirthDate() != null) {
            return registerBirth(id, pregnancy, form);
        }

        return updatePregnancy(id, pregnancy, form);
    }

    private Pessoa updatePregnancy(Long id, Pessoa pregnancy, PregnancyUpdateForm form) {
        if (form.getMotherId() == null) {
            throw new IllegalArgumentException("Mãe é obrigatória");
        }
        if (form.getFatherId() == null) {
            throw new IllegalArgumentException("Pai é obrigatório");
        }

        if (form.getExpectedDueDate() != null) {
            validateDueDateNotPast(form.getExpectedDueDate());
        }
        // Única transição que um diácono alcança: o resto já é 404 em loadPregnancy.
        if (Boolean.TRUE.equals(form.getConfidential())) requireConfidentialAccess();

        Pessoa mother = pessoaService.findById(form.getMotherId());
        Pessoa father = resolveFather(form.getFatherId(), form.getMotherId());

        String nickname = PregnancyNaming.buildNickname(mother, father);
        DateTime expectedDueDate = form.getExpectedDueDate() != null
                ? form.getExpectedDueDate()
                : pregnancy.getDataNascimento();
        String builtName = PregnancyNaming.buildName(form.getName(), nickname);

        pregnancy.setNome(builtName);
        pregnancy.setApelido(nickname);
        pregnancy.setDataNascimento(expectedDueDate);
        pregnancy.setSexo(form.getGender());
        if (form.getConfidential() != null) {
            pregnancy.setCategoria(form.getConfidential() ? CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO : CategoriaEnum.GESTACAO);
        }
        pregnancy.setChefeDeFamiliaId(resolveFamilyHead(mother));

        pessoaService.update(pregnancy);
        syncParentRelationships(pregnancy, mother.getId(), father.getId());

        return reloadWithRelationships(id);
    }

    private Pessoa registerBirth(Long id, Pessoa pregnancy, PregnancyUpdateForm form) {
        applyBirthRegistration(pregnancy, form);
        pessoaService.update(pregnancy);
        return reloadWithRelationships(id);
    }

    @Transactional
    public void close(Long id) {
        Pessoa pregnancy;
        try {
            pregnancy = pessoaService.findById(id);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Pessoa com ID " + id + " não encontrada");
        }

        if (!isPregnancy(pregnancy.getCategoria())) {
            throw new IllegalArgumentException("Só é possível encerrar registros de gestação");
        }
        if (officialActRepository.existsByPersonId(id)) {
            throw new IllegalStateException("Existem atos oficiais vinculados; não é possível excluir");
        }

        pessoaRepository.deletePregnancy(id);
    }

    private void applyBirthRegistration(Pessoa pregnancy, PregnancyUpdateForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("Nome completo é obrigatório");
        }
        if (form.getGender() == null) {
            throw new IllegalArgumentException("Sexo é obrigatório");
        }
        validateDateNotFuture(form.getBirthDate(), "Data de nascimento não pode ser futura");

        long categoryId = form.getTargetCategoryId() != null
                ? form.getTargetCategoryId()
                : DEFAULT_BIRTH_CATEGORY_ID;

        pregnancy.setNome(form.getName().trim());
        pregnancy.setApelido(PregnancyNaming.firstName(form.getName()));
        pregnancy.setDataNascimento(form.getBirthDate());
        pregnancy.setSexo(form.getGender());
        pregnancy.setCategoria(CategoriaEnum.fromId(categoryId));
    }

    /**
     * Filtrada de propósito: é o que dá 404 em editar, registrar nascimento e encerrar
     * sob sigilo. Não passe {@code INTERNAL} aqui.
     */
    private Pessoa loadPregnancy(Long id) {
        List<Pessoa> list = pessoaService.find(pregnancyQuery(id).build());
        if (list.isEmpty()) {
            throw new NoSuchElementException("Pessoa com ID " + id + " não encontrada");
        }
        Pessoa person = list.getFirst();
        if (!isPregnancy(person.getCategoria())) {
            throw new IllegalArgumentException("Operação permitida apenas para gestações (categorias 29 ou 30)");
        }
        return person;
    }

    /** Releitura após gravar: precisa enxergar a linha recém-escrita. */
    private Pessoa reloadWithRelationships(Long id) {
        List<Pessoa> list = pessoaService.find(pregnancyQuery(id).build(), ConfidentialAccess.INTERNAL);
        if (list.isEmpty()) {
            throw new NoSuchElementException("Pessoa com ID " + id + " não encontrada");
        }
        return list.getFirst();
    }

    private static PessoaQuery.Builder pregnancyQuery(Long id) {
        return PessoaQuery.builder()
                .id(id)
                .includes(PessoaInclude.RELACIONAMENTOS, PessoaInclude.CHEFE_DE_FAMILIA, PessoaInclude.ENDERECO);
    }

    private static void requireConfidentialAccess() {
        if (!SecurityUtils.hasAnyRole(Roles.elderNames())) {
            throw new AccessDeniedException("Apenas presbíteros e administradores gerenciam gestação em sigilo.");
        }
    }

    private static boolean isPregnancy(CategoriaEnum category) {
        return category == CategoriaEnum.GESTACAO || category == CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO;
    }

    private static Long resolveFamilyHead(Pessoa mother) {
        Long headId = mother.getChefeDeFamiliaId();
        if (headId == null || headId.equals(mother.getId())) {
            return mother.getId();
        }
        return headId;
    }

    private Pessoa resolveFather(Long fatherId, Long motherId) {
        if (fatherId == null) return null;
        if (fatherId.equals(motherId)) {
            throw new IllegalArgumentException("Pai deve ser diferente da mãe");
        }
        return pessoaService.findById(fatherId);
    }

    private void createParentRelationships(Long babyId, Long motherId, Long fatherId) {
        Relacionamento motherRel = new Relacionamento();
        motherRel.setPessoaRelacionadaId(motherId);
        motherRel.setTipoRelacionamento(TipoRelacionamento.MAE);
        pessoaService.createRelationship(babyId, motherRel);

        if (fatherId != null) {
            Relacionamento fatherRel = new Relacionamento();
            fatherRel.setPessoaRelacionadaId(fatherId);
            fatherRel.setTipoRelacionamento(TipoRelacionamento.PAI);
            pessoaService.createRelationship(babyId, fatherRel);
        }
    }

    private void syncParentRelationships(Pessoa pregnancy, Long motherId, Long fatherId) {
        List<Relacionamento> current = pregnancy.getRelacionamentos() != null
                ? pregnancy.getRelacionamentos()
                : List.of();

        List<Relacionamento> desired = new ArrayList<>();
        for (Relacionamento rel : current) {
            if (rel.getTipoRelacionamento() != TipoRelacionamento.MAE
                    && rel.getTipoRelacionamento() != TipoRelacionamento.PAI) {
                desired.add(rel);
            }
        }

        Relacionamento motherRel = new Relacionamento();
        motherRel.setPessoaRelacionadaId(motherId);
        motherRel.setTipoRelacionamento(TipoRelacionamento.MAE);
        desired.add(motherRel);

        if (fatherId != null) {
            Relacionamento fatherRel = new Relacionamento();
            fatherRel.setPessoaRelacionadaId(fatherId);
            fatherRel.setTipoRelacionamento(TipoRelacionamento.PAI);
            desired.add(fatherRel);
        }

        pessoaService.syncRelationships(pregnancy.getId(), current, desired);
    }

    private void applyFamilyHeadAddress(Pessoa person) {
        Long headId = person.getChefeDeFamiliaId();
        if (headId == null) return;

        List<Pessoa> heads = pessoaService.find(PessoaQuery.builder()
                .id(headId)
                .includes(PessoaInclude.ENDERECO)
                .build());
        if (heads.isEmpty()) return;

        if (heads.getFirst().getEndereco() != null) {
            person.setEndereco(heads.getFirst().getEndereco());
        }
    }

    private static void validateDueDateNotPast(DateTime expectedDueDate) {
        DateTime today = DateTime.now().withTimeAtStartOfDay();
        if (expectedDueDate.withTimeAtStartOfDay().isBefore(today)) {
            throw new IllegalArgumentException("DPP não pode ser anterior a hoje");
        }
    }

    private static void validateDateNotFuture(DateTime date, String message) {
        DateTime today = DateTime.now().withTimeAtStartOfDay();
        if (date.withTimeAtStartOfDay().isAfter(today)) {
            throw new IllegalArgumentException(message);
        }
    }
}
