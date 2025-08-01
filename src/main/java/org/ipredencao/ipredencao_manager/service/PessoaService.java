package org.ipredencao.ipredencao_manager.service;

import org.springframework.stereotype.Service;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;
import org.ipredencao.ipredencao_manager.model.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.SubcategoriaEnum;
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

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa create(Pessoa pessoa) {
        return pessoaRepository.insert(pessoa);
    }

    public Pessoa savePhoto(Pessoa pessoa, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(pessoa.getId(), foto);
        pessoa.setFotoUrl(fotoUrl);
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
        return pessoaRepository.update(pessoa);
    }

    // Relacionamentos qualificados
    public RelacionamentoPessoa criarRelacionamento(Long pessoaId, RelacionamentoPessoa relacionamento) {
        return pessoaRepository.insertRelationship(pessoaId, relacionamento);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return pessoaRepository.listarRelacionamentosPorPessoa(pessoaId);
    }
    
    /**
     * Lista todas as subcategorias disponíveis
     */
    public SubcategoriaEnum[] getAllSubcategorias() {
        return SubcategoriaEnum.values();
    }
}
