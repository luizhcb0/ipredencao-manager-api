package org.ipredencao.ipredencao_manager.config;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JodaModule jodaModule() {
        return new JodaModule();
    }
}
