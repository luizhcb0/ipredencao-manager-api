package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.springframework.stereotype.Service;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;

@Service
public class PessoaService {
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private SecurityUtils securityUtils;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa create(Pessoa pessoa) {
        // Definir quem criou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        return pessoaRepository.insert(pessoa);
    }

    public Pessoa savePhoto(Pessoa pessoa, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(pessoa.getId(), foto);
        pessoa.setFotoUrl(fotoUrl);
        
        // Definir quem atualizou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        
        return pessoaRepository.update(pessoa);
    }

    public Pessoa findById(Long id) {
        try {
            return pessoaRepository.find(PessoaQuery.builder().id(id).build()).getFirst();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Pessoa com ID " + id + " não encontrada");
        }
    }

    public List<Pessoa> find(PessoaQuery query) {
        return pessoaRepository.find(query);
    }

    public Pessoa update(Pessoa pessoa) {
        // Definir quem atualizou a pessoa
        Long currentUserId = securityUtils.getCurrentUserId();
        pessoa.setUpdatedByUserId(currentUserId);
        //TODO: Atualizar/Criar relacionamentos
        
        return pessoaRepository.update(pessoa);
    }

    // Relacionamento qualificados
    public Relacionamento createRelationship(Long pessoaId, Relacionamento relacionamento) {
        return pessoaRepository.insertRelationship(pessoaId, relacionamento);
    }
    
    /**
     * Busca o histórico de alterações de uma pessoa
     */
    public List<PessoaHistory> findHistoryById(Long id) {
        return pessoaRepository.findHistoryByPersonId(id);
    }
}
