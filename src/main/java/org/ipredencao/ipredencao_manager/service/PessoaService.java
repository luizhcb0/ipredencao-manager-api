package org.ipredencao.ipredencao_manager.service;

import org.springframework.stereotype.Service;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;
import org.ipredencao.ipredencao_manager.model.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.SubcategoriaEnum;
import java.util.List;
import java.util.NoSuchElementException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import com.amazonaws.services.s3.model.ObjectMetadata;

@Service
public class PessoaService {
    private final PessoaRepository pessoaRepository;

    @Autowired
    private AmazonS3 amazonS3;
    private final String bucketName = "ipredencao-manager-photos";

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa create(Pessoa pessoa) {
        return pessoaRepository.insert(pessoa);
    }

    public Pessoa savePhoto(Pessoa pessoa, MultipartFile foto) throws IOException {
        if (foto != null && !foto.isEmpty()) {
            String key = pessoa.getId() + "_" + System.currentTimeMillis() + "_" + foto.getOriginalFilename();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(foto.getSize());
            amazonS3.putObject(new PutObjectRequest(bucketName, key, foto.getInputStream(), metadata));
            String url = amazonS3.getUrl(bucketName, key).toString();
            pessoa.setFotoUrl(url);
            return pessoaRepository.update(pessoa);
        } else {
            throw new IllegalArgumentException("Foto não pode ser nula ou vazia");
        }
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
        return pessoaRepository.inserirRelacionamento(pessoaId, relacionamento);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return pessoaRepository.listarRelacionamentosPorPessoa(pessoaId);
    }
    
    /**
     * Define a subcategoria de uma pessoa usando o enum
     */
    public Pessoa setSubcategoria(Long pessoaId, SubcategoriaEnum subcategoriaEnum) {
        Pessoa pessoa = findById(pessoaId);
        pessoa.setSubcategoria(subcategoriaEnum);
        return pessoaRepository.update(pessoa);
    }
    
    /**
     * Busca pessoas por subcategoria
     */
    public List<Pessoa> findBySubcategoria(SubcategoriaEnum subcategoriaEnum) {
        // Busca direta no banco usando o ID da subcategoria
        return pessoaRepository.find(PessoaQuery.builder()
            .subcategoria(subcategoriaEnum)
            .build());
    }
    
    /**
     * Lista todas as subcategorias disponíveis
     */
    public SubcategoriaEnum[] getAllSubcategorias() {
        return SubcategoriaEnum.values();
    }
} 