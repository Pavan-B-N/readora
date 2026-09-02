package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.VirtualEditionNotFoundException;
import com.readora.catalog.exception.VirtualEditionNotOwnedException;
import com.readora.catalog.repository.VirtualEditionRepository;
import com.readora.catalog.storage.VirtualContentStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serves virtual-edition files for in-app reading only — never a public/downloadable link.
 * fileUrl on VirtualEdition is an opaque key into whichever VirtualContentStore is active (local
 * disk or Azure Blob Storage, per app.storage.provider) — this class only owns the
 * ownership/existence checks, not how or where the bytes are actually stored.
 */
@Service
public class VirtualContentService {

    private final VirtualEditionRepository virtualEditionRepository;
    private final CommerceClient commerceClient;
    private final VirtualContentStore contentStore;

    public VirtualContentService(
            VirtualEditionRepository virtualEditionRepository,
            CommerceClient commerceClient,
            VirtualContentStore contentStore
    ) {
        this.virtualEditionRepository = virtualEditionRepository;
        this.commerceClient = commerceClient;
        this.contentStore = contentStore;
    }

    @Transactional(readOnly = true)
    public Resource getContent(UUID userId, UUID bookId) {
        if (!isOwned(userId, bookId)) {
            throw new VirtualEditionNotOwnedException();
        }
        return resolveContent(bookId);
    }

    /**
     * Same file resolution as getContent(), but for trusted internal callers (ai-service's reader
     * embedding pipeline) that have already verified ownership themselves via isOwned() — skips
     * the ownership check since there's no end-user request context to check it against here.
     */
    @Transactional(readOnly = true)
    public Resource getContentForInternalUse(UUID bookId) {
        return resolveContent(bookId);
    }

    @Transactional(readOnly = true)
    public boolean isOwned(UUID userId, UUID bookId) {
        boolean hasActiveEdition = virtualEditionRepository.findById(bookId).filter(VirtualEdition::isActive).isPresent();
        return hasActiveEdition && commerceClient.getPurchasedBookIds(userId).contains(bookId);
    }

    private Resource resolveContent(UUID bookId) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId)
                .filter(VirtualEdition::isActive)
                .orElseThrow(VirtualEditionNotFoundException::new);

        return contentStore.resolve(edition.getFileUrl());
    }
}
