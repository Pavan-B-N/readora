package com.readora.commerce.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Every frontend treats money fields as strings, but Jackson serializes BigDecimal as a bare JSON number by default (e.g. 499.00 rather than "499.00"); serializing it as a plain string here keeps the wire format matching what every client already assumes.
@Configuration
public class JacksonConfig {

    // Registers the BigDecimal-as-string serializer on the shared ObjectMapper.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
