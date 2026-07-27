package com.readora.catalog.storage;

import org.springframework.core.io.Resource;

// Resolves a VirtualEdition's fileUrl (an opaque key, local filename or blob name depending on app.storage.provider) to a readable Resource; implementations own their own existence/safety checks and throw VirtualEditionNotFoundException, so VirtualContentService stays storage-backend-agnostic.
public interface VirtualContentStore {
    // Resolves the given opaque fileUrl key to a readable Resource.
    Resource resolve(String fileUrl);
}
