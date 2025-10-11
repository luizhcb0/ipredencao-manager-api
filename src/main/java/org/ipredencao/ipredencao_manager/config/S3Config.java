package org.ipredencao.ipredencao_manager.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    
    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        
        // Se endpoint está definido, usa LocalStack (desenvolvimento)
        if (endpoint != null && !endpoint.isEmpty()) {
            System.out.println("🔧 Configurando S3 para LocalStack: " + endpoint);
            builder.withEndpointConfiguration(
                    new AmazonS3ClientBuilder.EndpointConfiguration(endpoint, region))
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials("test", "test")));
        } else {
            // Usa S3 real da AWS (produção)
            System.out.println("☁️  Configurando S3 para AWS (região: " + region + ")");
            builder.withRegion(region)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        }
        
        return builder.build();
    }
} 