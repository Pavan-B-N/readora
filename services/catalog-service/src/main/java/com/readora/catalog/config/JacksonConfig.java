package com.readora.catalog.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Every frontend treats money fields as strings, but Jackson serializes BigDecimal as a bare JSON number by default (e.g. 499.00 vs "499.00"), which breaks the moment client code calls a string-only method on it — so this forces BigDecimal to serialize as a plain string to match what clients assume.
@Configuration
public class JacksonConfig {

    // Registers the BigDecimal-as-string serializer on the shared ObjectMapper.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
