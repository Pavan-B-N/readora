package com.readora.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record VirtualStorageProperties(Azure azure) {

    public VirtualStorageProperties {
        if (azure == null) {
            azure = new Azure(null, null);
        }
    }

    public record Azure(String connectionString, String containerName) {
    }
}
