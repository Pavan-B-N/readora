package com.readora.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the app.storage.* configuration properties for virtual-content storage.
@ConfigurationProperties(prefix = "app.storage")
public record VirtualStorageProperties(Azure azure) {

    // Defaults azure to an empty instance so downstream code never needs a null check.
    public VirtualStorageProperties {
        if (azure == null) {
            azure = new Azure(null, null);
        }
    }

    // Azure Blob Storage connection settings.
    public record Azure(String connectionString, String containerName) {
    }
}
