package com.readora.catalog.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.readora.catalog.config.VirtualStorageProperties;
import com.readora.catalog.exception.VirtualEditionNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// Reads virtual-edition files from Azure Blob Storage; the container client is built lazily on the first resolve() call (not at startup) so a missing/blank Azure config surfaces as a normal per-request error instead of preventing the whole service from starting up.
@Component
public class AzureBlobContentStore implements VirtualContentStore {

    private final VirtualStorageProperties properties;
    private final BlobContainerClient injectedContainerClient;

    // Constructs the store from injected storage properties; the container client itself is built lazily.
    @Autowired
    public AzureBlobContentStore(VirtualStorageProperties properties) {
        this.properties = properties;
        this.injectedContainerClient = null;
    }

    /** Test-only seam — lets tests supply a pre-built (mock) client and skip real Azure SDK construction. */
    AzureBlobContentStore(VirtualStorageProperties properties, BlobContainerClient containerClient) {
        this.properties = properties;
        this.injectedContainerClient = containerClient;
    }

    // Resolves a blob by name, throwing VirtualEditionNotFoundException if it doesn't exist.
    @Override
    public Resource resolve(String fileUrl) {
        BlobClient blobClient = containerClient().getBlobClient(fileUrl);
        if (!Boolean.TRUE.equals(blobClient.exists())) {
            throw new VirtualEditionNotFoundException();
        }
        // Streams lazily rather than buffering the whole blob into memory — openInputStream() reads from Blob Storage on demand as the HTTP response body is written.
        return new InputStreamResource(blobClient.openInputStream());
    }

    // Rebuilt per call rather than cached — buildClient() only assembles local objects (no network I/O), so the cost is negligible for an endpoint read occasionally, not worth the extra state/locking to avoid it.
    private BlobContainerClient containerClient() {
        return injectedContainerClient != null ? injectedContainerClient : buildContainerClient();
    }

    // Package-private (not private) so a test can exercise real Azure SDK client construction directly — no network call happens until an actual blob operation runs, so this is safe in a unit test.
    BlobContainerClient buildContainerClient() {
        VirtualStorageProperties.Azure azure = properties.azure();
        if (isBlank(azure.connectionString()) || isBlank(azure.containerName())) {
            throw new IllegalStateException(
                    "Azure Blob Storage is not configured — set AZURE_STORAGE_CONNECTION_STRING and AZURE_STORAGE_CONTAINER_NAME");
        }
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(azure.connectionString())
                .buildClient();
        return serviceClient.getBlobContainerClient(azure.containerName());
    }

    // Whether the given string is null or contains only whitespace.
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
