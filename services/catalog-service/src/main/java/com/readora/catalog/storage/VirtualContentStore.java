package com.readora.catalog.storage;

import org.springframework.core.io.Resource;

/**
 * Resolves a VirtualEdition's fileUrl (an opaque key — a local filename or a blob name,
 * depending on app.storage.provider) to a readable Resource. Implementations own their own
 * "does this actually exist / is it safe to read" checks and throw VirtualEditionNotFoundException
 * when they don't, so VirtualContentService stays storage-backend-agnostic.
 */
public interface VirtualContentStore {
    Resource resolve(String fileUrl);
}
