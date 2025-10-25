package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioRequest;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.ipredencao.ipredencao_manager.repository.EnderecoRepository;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FormularioPessoaService {
    @Autowired
    private FormularioPessoaRepository repository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private EnderecoRepository enderecoRepository;

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

    public List<FormularioPessoa> find(FormularioPessoaQuery query) {
        return repository.find(query);
    }

    @Transactional
    public ProcessarFormularioResponse processForm(ProcessarFormularioRequest request) {
        // 1. Buscar o formulário
        FormularioPessoa formulario = findById(request.getFormularioId());
        
        // 2. Processar pessoas dos relacionamentos primeiro
        List<Pessoa> relatedPeople = new ArrayList<>();
        
        // Processar pai
        if (formulario.getNomePai() != null && !formulario.getNomePai().trim().isEmpty()) {
            Pessoa father = processRelatedPerson(formulario.getNomePai());
            relatedPeople.add(father);
        }
        
        // Processar mãe
        if (formulario.getNomeMae() != null && !formulario.getNomeMae().trim().isEmpty()) {
            Pessoa mother = processRelatedPerson(formulario.getNomeMae());
            relatedPeople.add(mother);
        }
        
        // Processar pessoa relacionada
        Pessoa relatedPerson = null;
        if (formulario.getNomePessoaRelacionada() != null && !formulario.getNomePessoaRelacionada().trim().isEmpty()) {
            relatedPerson = processRelatedPerson(formulario.getNomePessoaRelacionada());
            relatedPeople.add(relatedPerson);
        }

        // Processar filhos
        List<Pessoa> children = new ArrayList<>();
        if (formulario.getNomeFilhos() != null && !formulario.getNomeFilhos().isEmpty()) {
            for (String nomeFilho : formulario.getNomeFilhos()) {
                if (nomeFilho != null && !nomeFilho.trim().isEmpty()) {
                    Pessoa child = processRelatedPerson(nomeFilho);
                    children.add(child);
                    relatedPeople.add(child);
                }
            }
        }

        // Processar chefe de família
        Pessoa familyHead = null;
        if (formulario.getChefeDeFamilia() != null && !formulario.getChefeDeFamilia().trim().isEmpty()) {
            familyHead = processFamilyHead(formulario.getChefeDeFamilia(), relatedPeople);
        }
        
        // 3. Criar ou atualizar a pessoa
        Pessoa person = createOrUpdatePerson(formulario, request.getPessoaId(), familyHead);
        
        // 4. Atualizar o formulário com o ID da pessoa criada/atualizada
        formulario.setPessoaId(person.getId());
        formulario.setStatus(FormPessoaStatus.VALIDADO);
        repository.update(formulario);
        
        // 5. Criar relacionamentos
        List<Relacionamento> relationships = createRelationships(person, formulario, relatedPerson, children, relatedPeople);
        
        String message = request.getPessoaId() != null ? 
            "Pessoa atualizada e relacionamentos criados com sucesso" : 
            "Pessoa criada e relacionamentos criados com sucesso";
            
        return new ProcessarFormularioResponse(person, relationships, message);
    }

    private Pessoa processRelatedPerson(String nameOrId) {
        // Verificar se é um ID (número)
        try {
            Long id = Long.parseLong(nameOrId.trim());
            return pessoaService.findById(id);
        } catch (NumberFormatException e) {
            // É um nome, criar nova pessoa
            Pessoa newPerson = new Pessoa();
            newPerson.setNome(nameOrId.trim());
            newPerson.setCategoria(CategoriaEnum.AGREGADO_FAMILIAR);
            return pessoaService.create(newPerson);
        }
    }

    private Pessoa createOrUpdatePerson(FormularioPessoa formulario, Long pessoaId, Pessoa familyHead) {
        Pessoa person;
        
        Endereco endereco = null;
        if (formulario.getPropagarEnderecoChefeFamilia() && familyHead != null && familyHead.getEndereco() != null) {
            endereco = familyHead.getEndereco();
        } else {
            endereco = new Endereco();
            endereco.setCep(formulario.getEnderecoCep());
            endereco.setLogradouro(formulario.getEnderecoLogradouro());
            endereco.setNumero(formulario.getEnderecoNumero());
            endereco.setComplemento(formulario.getEnderecoComplemento());
            
            // Tentar reutilizar endereço existente
            List<Endereco> enderecos = enderecoRepository.find(EnderecoQuery.builder()
                .cep(endereco.getCep())
                .build());
            
            if (!enderecos.isEmpty()) {
                endereco = enderecos.getFirst();
            } else {
                endereco = enderecoRepository.insert(endereco);
            }
        }
        
        if (pessoaId != null) {
            // Atualizar pessoa existente
            person = pessoaService.findById(pessoaId);
            mapFormToPerson(formulario, person);
            person.setEndereco(endereco);
            
            // Se tem chefe de família, definir
            if (familyHead != null) {
                person.setChefeDeFamiliaId(familyHead.getId());
            } else if (person.getChefeDeFamiliaId() == null) {
                // Se pessoa existente não tem chefe, ela é o próprio chefe
                person.setChefeDeFamiliaId(person.getId());
            }
            
            person = pessoaService.update(person);
        } else {
            // Criar nova pessoa
            person = new Pessoa();
            mapFormToPerson(formulario, person);
            person.setEndereco(endereco);

            // Se tem chefe de família, definir
            if (familyHead != null) {
                person.setChefeDeFamiliaId(familyHead.getId());
            }
            
            person = pessoaService.create(person);
            
            // Se pessoa nova não tem chefe, ela é o próprio chefe
            if (familyHead == null) {
                person.setChefeDeFamiliaId(person.getId());
                person = pessoaService.update(person);
            }
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
        pessoa.setRegiao(formulario.getRegiao());
        pessoa.setFotoUrl(formulario.getFotoUrl());
        pessoa.setSexo(formulario.getSexo());
        pessoa.setCategoria(formulario.getCategoria());
    }

    private List<Relacionamento> createRelationships(Pessoa pessoa, FormularioPessoa formulario,
                                                           Pessoa pessoaRelacionada, List<Pessoa> children, List<Pessoa> relatedPeople) {
        List<Relacionamento> relationships = new ArrayList<>();
        
        // Relacionamento com pessoa relacionada
        if (pessoaRelacionada != null) {
            TipoRelacionamento tipoRelacionamento = determineRelationshipType(formulario.getEstadoCivil());
            Relacionamento rel = new Relacionamento();
            rel.setPessoaId(pessoa.getId());
            rel.setPessoaRelacionadaId(pessoaRelacionada.getId());
            rel.setTipoRelacionamento(tipoRelacionamento);
            rel.setInicioRelacionamento(formulario.getInicioRelacionamento());
            relationships.add(pessoaService.createRelationship(pessoa.getId(), rel));
        }
        
        // Relacionamento com filhos
        for (Pessoa child : children) {
            Relacionamento rel = new Relacionamento();
            rel.setPessoaId(pessoa.getId());
            rel.setPessoaRelacionadaId(child.getId());
            rel.setTipoRelacionamento(TipoRelacionamento.FILHO);
            relationships.add(pessoaService.createRelationship(pessoa.getId(), rel));
        }
        
        // Relacionamento com pai e mãe
        for (Pessoa pessoaRel : relatedPeople) {
            if (formulario.getNomePai() != null && 
                (pessoaRel.getNome().equals(formulario.getNomePai()) || pessoaRel.getId().toString().equals(formulario.getNomePai()))) {
                Relacionamento rel = new Relacionamento();
                rel.setPessoaId(pessoa.getId());
                rel.setPessoaRelacionadaId(pessoaRel.getId());
                rel.setTipoRelacionamento(TipoRelacionamento.PAI);
                relationships.add(pessoaService.createRelationship(pessoa.getId(), rel));
            }
            
            if (formulario.getNomeMae() != null && 
                (pessoaRel.getNome().equals(formulario.getNomeMae()) || pessoaRel.getId().toString().equals(formulario.getNomeMae()))) {
                Relacionamento rel = new Relacionamento();
                rel.setPessoaId(pessoa.getId());
                rel.setPessoaRelacionadaId(pessoaRel.getId());
                rel.setTipoRelacionamento(TipoRelacionamento.MAE);
                relationships.add(pessoaService.createRelationship(pessoa.getId(), rel));
            }
        }
        
        return relationships;
    }

    private TipoRelacionamento determineRelationshipType(EstadoCivil estadoCivil) {
        return switch (estadoCivil) {
            case CASADO -> TipoRelacionamento.CONJUGE;
            case SOLTEIRO_NAMORANDO, DIVORCIADO_NAMORANDO, VIUVO_NAMORANDO -> TipoRelacionamento.NAMORADO;
            case SOLTEIRO_NOIVO, VIUVO_NOIVO, DIVORCIADO_NOIVO -> TipoRelacionamento.NOIVO;
            case VIUVO_SEM_RELACIONAMENTO -> TipoRelacionamento.VIUVO;
            default -> TipoRelacionamento.SEM_RELACIONAMENTO;
        };
    }
    
    private Pessoa processFamilyHead(String nameOrId, List<Pessoa> relatedPeople) {
        // Verificar se é um ID (número)
        try {
            Long id = Long.parseLong(nameOrId.trim());
            return pessoaService.findById(id);
        } catch (NumberFormatException e) {
            // É um nome, verificar se já foi criado nos relacionamentos
            for (Pessoa person : relatedPeople) {
                if (person.getNome() != null && person.getNome().trim().equalsIgnoreCase(nameOrId.trim())) {
                    return person;
                }
            }
            
            // Se não encontrou, criar nova pessoa
            Pessoa newPerson = new Pessoa();
            newPerson.setNome(nameOrId.trim());
            newPerson.setCategoria(CategoriaEnum.AGREGADO_FAMILIAR);
            Pessoa createdPerson = pessoaService.create(newPerson);
            relatedPeople.add(createdPerson);
            return createdPerson;
        }
    }
}
