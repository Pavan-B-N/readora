package com.readora.catalog.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.readora.catalog.config.VirtualStorageProperties;
import com.readora.catalog.exception.VirtualEditionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobContentStoreTest {

    @Mock private BlobContainerClient containerClient;
    @Mock private BlobClient blobClient;
    @Mock private BlobInputStream blobInputStream;

    private AzureBlobContentStore store;

    @BeforeEach
    void setUp() {
        store = new AzureBlobContentStore(configuredProperties(), containerClient);
    }

    private static VirtualStorageProperties configuredProperties() {
        return new VirtualStorageProperties(new VirtualStorageProperties.Azure("UseDevelopmentStorage=true", "readora-virtual-editions"));
    }

    @Test
    void resolve_blobExists_returnsStreamingResource() throws Exception {
        when(containerClient.getBlobClient("book.pdf")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.openInputStream()).thenReturn(blobInputStream);

        Resource resource = store.resolve("book.pdf");

        assertThat(resource.getInputStream()).isSameAs((InputStream) blobInputStream);
    }

    @Test
    void resolve_blobMissing_throwsNotFound() {
        when(containerClient.getBlobClient("missing.pdf")).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(false);

        assertThatThrownBy(() -> store.resolve("missing.pdf")).isInstanceOf(VirtualEditionNotFoundException.class);
    }

    @Test
    void resolve_connectionStringNotConfigured_throwsCleanlyInsteadOfCrashingAtStartup() {
        VirtualStorageProperties unconfigured = new VirtualStorageProperties(new VirtualStorageProperties.Azure(null, "readora-virtual-editions"));
        AzureBlobContentStore unconfiguredStore = new AzureBlobContentStore(unconfigured);

        assertThatThrownBy(() -> unconfiguredStore.resolve("book.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void resolve_containerNameNotConfigured_throwsCleanly() {
        VirtualStorageProperties unconfigured = new VirtualStorageProperties(new VirtualStorageProperties.Azure("UseDevelopmentStorage=true", " "));
        AzureBlobContentStore unconfiguredStore = new AzureBlobContentStore(unconfigured);

        assertThatThrownBy(() -> unconfiguredStore.resolve("book.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void buildContainerClient_configured_constructsARealClientWithoutNetworkIO() {
        // buildClient()/getBlobContainerClient() only assemble local client/pipeline objects — no
        // network call happens until an actual blob operation runs — so this exercises the real
        // Azure SDK construction path deterministically, no emulator or live account needed.
        AzureBlobContentStore realStore = new AzureBlobContentStore(configuredProperties());

        assertThat(realStore.buildContainerClient()).isNotNull();
    }
}
