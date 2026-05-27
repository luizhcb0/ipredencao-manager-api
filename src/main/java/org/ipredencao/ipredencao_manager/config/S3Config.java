package org.ipredencao.ipredencao_manager.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

        if (endpoint != null && !endpoint.isEmpty()) {
            log.info("Configurando S3 para LocalStack: {}", endpoint);
            builder.withEndpointConfiguration(
                    new AmazonS3ClientBuilder.EndpointConfiguration(endpoint, region))
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials("test", "test")));
        } else {
            log.info("Configurando S3 para AWS (regiao: {})", region);
            builder.withRegion(region)
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        }

        return builder.build();
    }
} 