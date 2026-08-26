package com.readora.payment.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Every frontend treats money fields as strings (avoids float-precision surprises on the client),
 * but Jackson serializes BigDecimal as a bare JSON number by default — e.g. {@code 499.00} rather
 * than {@code "499.00"}. That mismatch is silently masked wherever a value is just interpolated
 * into a template string, but breaks outright the moment client code calls a string-only method
 * on it. Serializing BigDecimal as a plain string here keeps the wire format matching what every
 * client already assumes.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
