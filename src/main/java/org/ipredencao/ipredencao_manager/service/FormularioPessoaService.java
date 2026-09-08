package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioRequest;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.ipredencao.ipredencao_manager.repository.EnderecoRepository;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
public class FormularioPessoaService {
    private static final Logger logger = LoggerFactory.getLogger(FormularioPessoaService.class);

    @Autowired
    private FormularioPessoaRepository repository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    public FormularioPessoa create(FormularioPessoa formulario) {
        return repository.insert(formulario);
    }

    public FormularioPessoa savePhoto(FormularioPessoa formulario, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(formulario.getId(), foto);
        formulario.setFotoUrl(fotoUrl);
        return repository.update(formulario);
    }

    public FormularioPessoa update(FormularioPessoa formulario) {
        return repository.update(formulario);
    }

    public FormularioPessoa findById(Long id) {
        try {
            return repository.find(FormularioPessoaQuery.builder().id(id).build()).getFirst();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Formulario Pessoa com ID " + id + " não encontrada");
        }
    }

    public PagedResponse<FormularioPessoa> findPaginated(FormularioPessoaQuery query) {
        if (query.getPagination() == null) query.setPagination(new PaginationParameters());
        query.getPagination().applyDefaults();

        List<FormularioPessoa> formularios = repository.find(query);
        long total = repository.count(query);

        PageInfo pageInfo = new PageInfo(
            query.getPagination().getLimit(),
            query.getPagination().getOffset(),
            total
        );

        return new PagedResponse<>(formularios, pageInfo);
    }

    public ProcessarFormularioResponse processForm(ProcessarFormularioRequest request) {
        ProcessarFormularioResponse persisted = Objects.requireNonNull(
            new TransactionTemplate(transactionManager).execute(status -> persistProcessedForm(request)));
        try {
            userService.ensureInactiveUserForPerson(persisted.pessoaPrincipal());
            return persisted;
        } catch (Exception e) {
            logger.warn("Falha ao provisionar usuário para pessoa {}: {}",
                persisted.pessoaPrincipal().getId(), e.getMessage());
            return new ProcessarFormularioResponse(
                persisted.pessoaPrincipal(),
                persisted.relacionamentosCriados(),
                UserService.USER_PROVISION_WARNING);
        }
    }

    private ProcessarFormularioResponse persistProcessedForm(ProcessarFormularioRequest request) {
        FormularioPessoa formulario = findById(request.getFormularioId());
        String campus = formulario.getCampus();

        Endereco endereco = resolveEndereco(formulario);

        Pessoa familyHead = null;
        if (hasText(formulario.getChefeDeFamilia())) {
            familyHead = findOrCreateFamilyHead(formulario.getChefeDeFamilia(), campus, endereco);
            // Se a flag de propagação está ativa e o chefe existente tem endereço próprio, a pessoa principal herda.
            if (Boolean.TRUE.equals(formulario.getPropagarEnderecoChefeFamilia()) && familyHead.getEndereco() != null) {
                endereco = familyHead.getEndereco();
            }
        }

        Pessoa person = createOrUpdatePerson(formulario, request.getPessoaId(), familyHead, endereco);

        Pessoa father = hasText(formulario.getNomePai())
            ? findOrCreateReferencedPerson(formulario.getNomePai(), campus) : null;
        Pessoa mother = hasText(formulario.getNomeMae())
            ? findOrCreateReferencedPerson(formulario.getNomeMae(), campus) : null;
        Pessoa spouse = hasText(formulario.getNomePessoaRelacionada())
            ? findOrCreateReferencedPerson(formulario.getNomePessoaRelacionada(), campus) : null;

        List<Pessoa> children = new ArrayList<>();
        if (formulario.getNomeFilhos() != null) {
            for (String nomeFilho : formulario.getNomeFilhos()) {
                if (hasText(nomeFilho)) {
                    children.add(findOrCreateReferencedPerson(nomeFilho, campus));
                }
            }
        }

        formulario.setPessoaId(person.getId());
        formulario.setStatus(FormPessoaStatus.VALIDADO);
        repository.update(formulario);

        List<Relacionamento> relationships = createRelationships(person, formulario, father, mother, spouse, children);

        String message = request.getPessoaId() != null ?
            "Pessoa atualizada e relacionamentos criados com sucesso" :
            "Pessoa criada e relacionamentos criados com sucesso";

        return new ProcessarFormularioResponse(person, relationships, message);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Endereco resolveEndereco(FormularioPessoa formulario) {
        Endereco endereco = new Endereco();
        endereco.setCep(formulario.getEnderecoCep());
        endereco.setLogradouro(formulario.getEnderecoLogradouro());
        endereco.setNumero(formulario.getEnderecoNumero());
        endereco.setComplemento(formulario.getEnderecoComplemento());

        List<Endereco> existentes = enderecoRepository.find(EnderecoQuery.builder()
            .cep(endereco.getCep())
            .build());

        if (!existentes.isEmpty()) {
            return existentes.getFirst();
        }
        return enderecoRepository.insert(endereco);
    }

    /** Valor numérico é id de pessoa existente; qualquer outro texto vira pessoa referenciada, chefe de si mesma. */
    private Pessoa findOrCreateFamilyHead(String nameOrId, String campus, Endereco endereco) {
        Long id = asId(nameOrId);
        if (id != null) {
            return pessoaService.findById(id);
        }

        Pessoa newPerson = buildReferencedPerson(nameOrId, campus);
        newPerson.setEndereco(endereco);
        Pessoa created = pessoaService.create(newPerson);
        created.setChefeDeFamiliaId(created.getId());
        return pessoaService.update(created);
    }

    /** Valor numérico é id de pessoa existente; qualquer outro texto vira pessoa referenciada. */
    private Pessoa findOrCreateReferencedPerson(String nameOrId, String campus) {
        Long id = asId(nameOrId);
        return id != null
            ? pessoaService.findById(id)
            : pessoaService.create(buildReferencedPerson(nameOrId, campus));
    }

    private static Long asId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Pessoa buildReferencedPerson(String nome, String campus) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome.trim());
        pessoa.setCategoria(CategoriaEnum.PESSOA_REFERENCIADA);
        pessoa.setCampus(campus);
        return pessoa;
    }

    private Pessoa createOrUpdatePerson(FormularioPessoa formulario, Long pessoaId, Pessoa familyHead, Endereco endereco) {
        Pessoa person = pessoaId != null ? pessoaService.findById(pessoaId) : new Pessoa();
        mapFormToPerson(formulario, person);
        person.setEndereco(endereco);

        if (familyHead != null) {
            person.setChefeDeFamiliaId(familyHead.getId());
        }

        if (pessoaId != null) {
            if (familyHead == null && person.getChefeDeFamiliaId() == null) {
                person.setChefeDeFamiliaId(person.getId());
            }
            return pessoaService.update(person);
        }

        person = pessoaService.create(person);
        if (familyHead == null) {
            person.setChefeDeFamiliaId(person.getId());
            return pessoaService.update(person);
        }
        return person;
    }

    private void mapFormToPerson(FormularioPessoa formulario, Pessoa pessoa) {
        pessoa.setNome(formulario.getNome());
        pessoa.setApelido(formulario.getApelido());
        pessoa.setEmail(formulario.getEmail());
        pessoa.setEmailsSecundarios(formulario.getEmailsSecundarios());
        pessoa.setTelefone(formulario.getTelefone());
        pessoa.setTelefonesSecundarios(formulario.getTelefonesSecundarios());
        pessoa.setCampus(formulario.getCampus());
        pessoa.setDataNascimento(formulario.getDataNascimento());
        pessoa.setCpf(formulario.getCpf());
        pessoa.setRg(formulario.getRg());
        pessoa.setEstadoCivil(formulario.getEstadoCivil());
        pessoa.setIgrejaAnterior(formulario.getIgrejaAnterior());
        pessoa.setMotivosParaAdmissao(formulario.getMotivosParaAdmissao());
        pessoa.setTipoBatismo(formulario.getTipoBatismo());
        pessoa.setDataBatismo(formulario.getDataBatismo());
        pessoa.setDataProfissaoDeFe(formulario.getDataProfissaoDeFe());
        pessoa.setIgrejaBatismo(formulario.getIgrejaBatismo());
        pessoa.setProfissao(formulario.getProfissao());
        pessoa.setEmpresa(formulario.getEmpresa());
        pessoa.setFotoUrl(formulario.getFotoUrl());
        pessoa.setSexo(formulario.getSexo());
        pessoa.setCategoria(formulario.getCategoria());
    }

    private List<Relacionamento> createRelationships(Pessoa pessoa, FormularioPessoa formulario,
                                                     Pessoa father, Pessoa mother, Pessoa spouse, List<Pessoa> children) {
        List<Relacionamento> relationships = new ArrayList<>();

        if (spouse != null) {
            // Estados como SOLTEIRO_SEM_RELACIONAMENTO não geram relacionamento, mesmo se o campo vier preenchido.
            determineRelationshipType(formulario.getEstadoCivil())
                .ifPresent(tipo -> relationships.add(
                    persistRelationship(pessoa, spouse, tipo, formulario.getInicioRelacionamento())));
        }
        for (Pessoa child : children) {
            relationships.add(persistRelationship(pessoa, child, TipoRelacionamento.FILHO, null));
        }
        if (father != null) {
            relationships.add(persistRelationship(pessoa, father, TipoRelacionamento.PAI, null));
        }
        if (mother != null) {
            relationships.add(persistRelationship(pessoa, mother, TipoRelacionamento.MAE, null));
        }

        return relationships;
    }

    private Relacionamento persistRelationship(Pessoa pessoa, Pessoa related, TipoRelacionamento tipo, DateTime inicio) {
        Relacionamento rel = new Relacionamento();
        rel.setPessoaId(pessoa.getId());
        rel.setPessoaRelacionadaId(related.getId());
        rel.setTipoRelacionamento(tipo);
        if (inicio != null) {
            rel.setInicioRelacionamento(inicio);
        }
        return pessoaService.createRelationship(pessoa.getId(), rel);
    }

    private Optional<TipoRelacionamento> determineRelationshipType(EstadoCivil estadoCivil) {
        return switch (estadoCivil) {
            case CASADO -> Optional.of(TipoRelacionamento.CONJUGE);
            case SOLTEIRO_NAMORANDO, DIVORCIADO_NAMORANDO, VIUVO_NAMORANDO -> Optional.of(TipoRelacionamento.NAMORADO);
            case SOLTEIRO_NOIVO, VIUVO_NOIVO, DIVORCIADO_NOIVO -> Optional.of(TipoRelacionamento.NOIVO);
            case SOLTEIRO_SEM_RELACIONAMENTO,
                 DIVORCIADO_SEM_RELACIONAMENTO,
                 VIUVO_SEM_RELACIONAMENTO -> Optional.empty();
        };
    }

}
