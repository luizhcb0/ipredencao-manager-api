package org.ipredencao.ipredencao_manager.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.service-account-key-path:#{null}}")
    private String serviceAccountKeyPath;
    
    @Value("${firebase.service-account-key-content:#{null}}")
    private String serviceAccountKeyContent;
    
    @PostConstruct
    public void initializeFirebase() {
        try {
            GoogleCredentials credentials = getCredentials();
            
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase inicializado com sucesso");
            }
        } catch (IOException e) {
            log.error("Erro ao inicializar Firebase", e);
            throw new RuntimeException("Falha na inicialização do Firebase", e);
        }
    }
    
    private GoogleCredentials getCredentials() throws IOException {
        // Prioridade: 1) Conteúdo direto, 2) Arquivo, 3) Default credentials
        if (serviceAccountKeyContent != null && !serviceAccountKeyContent.isEmpty()) {
            log.info("Usando Firebase service account key do conteúdo da variável de ambiente");
            return GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKeyContent.getBytes())
            );
        }
        
        if (serviceAccountKeyPath != null && !serviceAccountKeyPath.isEmpty()) {
            log.info("Usando Firebase service account key do arquivo: {}", serviceAccountKeyPath);
            
            // Se o caminho for relativo ao classpath (resources), usar getResourceAsStream
            if (serviceAccountKeyPath.startsWith("src/main/resources/")) {
                String resourcePath = serviceAccountKeyPath.replace("src/main/resources/", "");
                var resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (resourceStream != null) {
                    return GoogleCredentials.fromStream(resourceStream);
                }
            }
            
            // Caso contrário, usar como caminho absoluto
            return GoogleCredentials.fromStream(
                new FileInputStream(serviceAccountKeyPath)
            );
        }
        
        // Fallback para Application Default Credentials (útil em produção)
        log.info("Usando Firebase Application Default Credentials");
        return GoogleCredentials.getApplicationDefault();
    }
}
