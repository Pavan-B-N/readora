package com.readora.user.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Every frontend treats money fields as strings, but Jackson serializes BigDecimal as a bare JSON number by default; this forces BigDecimal to serialize as a plain string to match what every client already assumes.
@Configuration
public class JacksonConfig {

    // Registers a Jackson customizer that serializes BigDecimal as a string instead of a number.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
