package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.joda.time.DateTime;

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

    public FormularioPessoa criar(FormularioPessoa formulario) {
        // Validar se o email já existe
        if (!repository.find(FormularioPessoaQuery.builder().email(formulario.getEmail()).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um formulário cadastrado com este email: " + formulario.getEmail());
        }
        return repository.insert(formulario);
    }

    public FormularioPessoa savePhoto(FormularioPessoa formulario, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(formulario.getId(), foto);
        formulario.setFotoUrl(fotoUrl);
        return repository.update(formulario);
    }

    public FormularioPessoa atualizar(FormularioPessoa formulario) {
        // Validar se o email já existe para outro formulário
        if (formulario.getId() != null && !repository.find(FormularioPessoaQuery.builder().email(formulario.getEmail()).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe outro formulário cadastrado com este email: " + formulario.getEmail());
        }
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

    public ProcessarFormularioResponse processarFormulario(ProcessarFormularioRequest request) {
        // 1. Buscar o formulário
        FormularioPessoa formulario = findById(request.getFormularioId());
        
        // 2. Processar pessoas dos relacionamentos primeiro
        List<Pessoa> pessoasRelacionamentos = new ArrayList<>();
        
        // Processar pai
        if (formulario.getNomePai() != null && !formulario.getNomePai().trim().isEmpty()) {
            Pessoa pai = processarPessoaRelacionamento(formulario.getNomePai());
            pessoasRelacionamentos.add(pai);
        }
        
        // Processar mãe
        if (formulario.getNomeMae() != null && !formulario.getNomeMae().trim().isEmpty()) {
            Pessoa mae = processarPessoaRelacionamento(formulario.getNomeMae());
            pessoasRelacionamentos.add(mae);
        }
        
        // Processar parceiro
        Pessoa parceiro = null;
        if (formulario.getNomeParceiro() != null && !formulario.getNomeParceiro().trim().isEmpty()) {
            parceiro = processarPessoaRelacionamento(formulario.getNomeParceiro());
            pessoasRelacionamentos.add(parceiro);
        }
        
        // Processar chefe de família
        Pessoa chefeFamilia = null;
        if (formulario.getChefeDeFamiliaId() != null) {
            try {
                chefeFamilia = pessoaService.findById(formulario.getChefeDeFamiliaId());
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Chefe de família com ID " + formulario.getChefeDeFamiliaId() + " não encontrado");
            }
        }
        
        // Processar filhos
        List<Pessoa> filhos = new ArrayList<>();
        if (formulario.getNomeFilhos() != null && !formulario.getNomeFilhos().isEmpty()) {
            for (String nomeFilho : formulario.getNomeFilhos()) {
                if (nomeFilho != null && !nomeFilho.trim().isEmpty()) {
                    Pessoa filho = processarPessoaRelacionamento(nomeFilho);
                    filhos.add(filho);
                    pessoasRelacionamentos.add(filho);
                }
            }
        }
        
        // 3. Criar ou atualizar a pessoa
        Pessoa pessoa = criarOuAtualizarPessoa(formulario, request.getPessoaId(), chefeFamilia);
        
        // 4. Atualizar o formulário com o ID da pessoa criada/atualizada
        formulario.setPessoaId(pessoa.getId());
        repository.update(formulario);
        
        // 5. Criar relacionamentos
        List<RelacionamentoPessoa> relacionamentos = criarRelacionamentos(pessoa, formulario, parceiro, filhos, pessoasRelacionamentos);
        
        String mensagem = request.getPessoaId() != null ? 
            "Pessoa atualizada e relacionamentos criados com sucesso" : 
            "Pessoa criada e relacionamentos criados com sucesso";
            
        return new ProcessarFormularioResponse(pessoa, relacionamentos, mensagem);
    }

    private Pessoa processarPessoaRelacionamento(String nomeOuId) {
        // Verificar se é um ID (número)
        try {
            Long id = Long.parseLong(nomeOuId.trim());
            return pessoaService.findById(id);
        } catch (NumberFormatException e) {
            // É um nome, criar nova pessoa
            Pessoa novaPessoa = new Pessoa();
            novaPessoa.setNome(nomeOuId.trim());
            novaPessoa.setAddedAt(DateTime.now());
            novaPessoa.setUpdatedAt(DateTime.now());
            return pessoaService.create(novaPessoa);
        }
    }

    private Pessoa criarOuAtualizarPessoa(FormularioPessoa formulario, Long pessoaId, Pessoa chefeFamilia) {
        Pessoa pessoa;
        
        if (pessoaId != null) {
            // Atualizar pessoa existente
            pessoa = pessoaService.findById(pessoaId);
            mapearFormularioParaPessoa(formulario, pessoa);
            pessoa.setUpdatedAt(DateTime.now());
            pessoa = pessoaService.update(pessoa);
        } else {
            // Criar nova pessoa
            pessoa = new Pessoa();
            mapearFormularioParaPessoa(formulario, pessoa);
            pessoa.setAddedAt(DateTime.now());
            pessoa.setUpdatedAt(DateTime.now());
            
            // Se tem chefe de família, definir
            if (chefeFamilia != null) {
                pessoa.setChefeDeFamiliaId(chefeFamilia.getId());
            }
            
            pessoa = pessoaService.create(pessoa);
        }
        
        // Propagar endereço do chefe de família se solicitado
        if (Boolean.TRUE.equals(formulario.getPropagarEnderecoChefeFamilia()) && chefeFamilia != null) {
            pessoa.setEndereco(chefeFamilia.getEndereco());
            pessoa.setCep(chefeFamilia.getCep());
            pessoa.setLatitude(chefeFamilia.getLatitude());
            pessoa.setLongitude(chefeFamilia.getLongitude());
            pessoa.setRegiao(chefeFamilia.getRegiao());
            pessoa = pessoaService.update(pessoa);
        }
        
        return pessoa;
    }

    private void mapearFormularioParaPessoa(FormularioPessoa formulario, Pessoa pessoa) {
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
        pessoa.setSituacaoIgrejaAnterior(formulario.getSituacaoIgrejaAnterior());
        pessoa.setTempoNaIgreja(formulario.getTempoNaIgreja());
        pessoa.setMotivosParaAdmissao(formulario.getMotivosParaAdmissao());
        pessoa.setTipoBatismo(formulario.getTipoBatismo());
        pessoa.setDataBatismo(formulario.getDataBatismo());
        pessoa.setDataProfissaoDeFe(formulario.getDataProfissaoDeFe());
        pessoa.setIgrejaBatismo(formulario.getIgrejaBatismo());
        pessoa.setProfissao(formulario.getProfissao());
        pessoa.setEmpresa(formulario.getEmpresa());
        pessoa.setEndereco(formulario.getEndereco());
        pessoa.setCep(formulario.getCep());
        pessoa.setRegiao(formulario.getRegiao());
        pessoa.setLatitude(formulario.getLatitude());
        pessoa.setLongitude(formulario.getLongitude());
        pessoa.setFotoUrl(formulario.getFotoUrl());
        pessoa.setSexo(formulario.getSexo());
        pessoa.setSubcategoria(formulario.getSubcategoria());
    }

    private List<RelacionamentoPessoa> criarRelacionamentos(Pessoa pessoa, FormularioPessoa formulario, 
                                                           Pessoa parceiro, List<Pessoa> filhos, List<Pessoa> pessoasRelacionamentos) {
        List<RelacionamentoPessoa> relacionamentos = new ArrayList<>();
        
        // Relacionamento com parceiro
        if (parceiro != null) {
            TipoRelacionamento tipoRelacionamento = determinarTipoRelacionamentoParceiro(formulario.getEstadoCivil());
            RelacionamentoPessoa rel = new RelacionamentoPessoa();
            rel.setPessoaId(pessoa.getId());
            rel.setPessoaRelacionadaId(parceiro.getId());
            rel.setTipoRelacionamento(tipoRelacionamento);
            rel.setAddedAt(DateTime.now());
            rel.setUpdatedAt(DateTime.now());
            relacionamentos.add(pessoaService.criarRelacionamento(pessoa.getId(), rel));
        }
        
        // Relacionamentos com filhos
        for (Pessoa filho : filhos) {
            RelacionamentoPessoa rel = new RelacionamentoPessoa();
            rel.setPessoaId(pessoa.getId());
            rel.setPessoaRelacionadaId(filho.getId());
            rel.setTipoRelacionamento(TipoRelacionamento.FILHO);
            rel.setAddedAt(DateTime.now());
            rel.setUpdatedAt(DateTime.now());
            relacionamentos.add(pessoaService.criarRelacionamento(pessoa.getId(), rel));
        }
        
        // Relacionamentos com pai e mãe
        for (Pessoa pessoaRel : pessoasRelacionamentos) {
            if (formulario.getNomePai() != null && 
                (pessoaRel.getNome().equals(formulario.getNomePai()) || pessoaRel.getId().toString().equals(formulario.getNomePai()))) {
                RelacionamentoPessoa rel = new RelacionamentoPessoa();
                rel.setPessoaId(pessoa.getId());
                rel.setPessoaRelacionadaId(pessoaRel.getId());
                rel.setTipoRelacionamento(TipoRelacionamento.PAI);
                rel.setAddedAt(DateTime.now());
                rel.setUpdatedAt(DateTime.now());
                relacionamentos.add(pessoaService.criarRelacionamento(pessoa.getId(), rel));
            }
            
            if (formulario.getNomeMae() != null && 
                (pessoaRel.getNome().equals(formulario.getNomeMae()) || pessoaRel.getId().toString().equals(formulario.getNomeMae()))) {
                RelacionamentoPessoa rel = new RelacionamentoPessoa();
                rel.setPessoaId(pessoa.getId());
                rel.setPessoaRelacionadaId(pessoaRel.getId());
                rel.setTipoRelacionamento(TipoRelacionamento.MAE);
                rel.setAddedAt(DateTime.now());
                rel.setUpdatedAt(DateTime.now());
                relacionamentos.add(pessoaService.criarRelacionamento(pessoa.getId(), rel));
            }
        }
        
        return relacionamentos;
    }

    private TipoRelacionamento determinarTipoRelacionamentoParceiro(EstadoCivil estadoCivil) {
        if (estadoCivil == null) {
            return TipoRelacionamento.NAMORADO; // padrão
        }
        
        switch (estadoCivil) {
            case CASADO:
                return TipoRelacionamento.CONJUGE;
            case NOIVO:
                return TipoRelacionamento.NOIVO;
            case NAMORANDO:
                return TipoRelacionamento.NAMORADO;
            default:
                return TipoRelacionamento.NAMORADO;
        }
    }
} 