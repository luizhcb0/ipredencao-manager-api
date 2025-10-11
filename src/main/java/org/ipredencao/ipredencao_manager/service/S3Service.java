package org.ipredencao.ipredencao_manager.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Service
public class S3Service {
    
    @Autowired
    private AmazonS3 amazonS3;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @PostConstruct
    public void initializeBucket() {
        try {
            // Verifica se o bucket existe, se não, cria
            if (!amazonS3.doesBucketExistV2(bucketName)) {
                amazonS3.createBucket(new CreateBucketRequest(bucketName));
                System.out.println("Bucket criado: " + bucketName);
            } else {
                System.out.println("Bucket já existe: " + bucketName);
            }
        } catch (Exception e) {
            System.err.println("Erro ao inicializar bucket: " + e.getMessage());
            // Não lança exceção para não impedir a inicialização da aplicação
        }
    }

    public String uploadFile(String fileName, MultipartFile file) throws IOException {
        // Garante que o bucket existe antes de fazer upload
        ensureBucketExists();
        
        String key = System.currentTimeMillis() + "_" + fileName;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        amazonS3.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata));
        return amazonS3.getUrl(bucketName, key).toString();
    }

    public String uploadPhoto(Long id, MultipartFile photo) throws IOException {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("Foto não pode ser nula ou vazia");
        }
        
        String fileName = id + "_" + photo.getOriginalFilename() + "_" + System.currentTimeMillis();
        return uploadFile(fileName, photo);
    }

    private void ensureBucketExists() {
        try {
            if (!amazonS3.doesBucketExistV2(bucketName)) {
                amazonS3.createBucket(new CreateBucketRequest(bucketName));
                System.out.println("Bucket criado durante operação: " + bucketName);
            }
        } catch (Exception e) {
            System.err.println("Erro ao verificar/criar bucket: " + e.getMessage());
            throw new RuntimeException("Erro ao acessar o serviço de armazenamento", e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Extrai a chave do arquivo da URL
            String key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            amazonS3.deleteObject(bucketName, key);
        } catch (Exception e) {
            System.err.println("Erro ao deletar arquivo: " + e.getMessage());
        }
    }
} 