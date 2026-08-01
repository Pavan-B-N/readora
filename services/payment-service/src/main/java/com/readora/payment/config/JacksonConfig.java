package com.readora.payment.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Every frontend treats money fields as strings (avoids float-precision surprises on the client), but Jackson serializes BigDecimal as a bare JSON number by default (e.g. 499.00 rather than "499.00"), a mismatch that's silently masked in template interpolation but breaks outright when client code calls a string-only method on it — serializing BigDecimal as a plain string here keeps the wire format matching what every client already assumes.
@Configuration
public class JacksonConfig {

    // Registers the BigDecimal-as-string serializer customizer.
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
