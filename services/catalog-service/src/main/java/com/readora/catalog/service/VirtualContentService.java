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

// Serves virtual-edition files for in-app reading only, never a public/downloadable link — owns only the ownership/existence checks; fileUrl is an opaque key resolved by whichever VirtualContentStore is active (local disk or Azure Blob Storage).
@Service
public class VirtualContentService {

    private final VirtualEditionRepository virtualEditionRepository;
    private final CommerceClient commerceClient;
    private final VirtualContentStore contentStore;

    // Wires in the repository, commerce client, and content store this service depends on.
    public VirtualContentService(
            VirtualEditionRepository virtualEditionRepository,
            CommerceClient commerceClient,
            VirtualContentStore contentStore
    ) {
        this.virtualEditionRepository = virtualEditionRepository;
        this.commerceClient = commerceClient;
        this.contentStore = contentStore;
    }

    // Resolves a virtual edition's file for a user, after verifying they own it.
    @Transactional(readOnly = true)
    public Resource getContent(UUID userId, UUID bookId) {
        if (!isOwned(userId, bookId)) {
            throw new VirtualEditionNotOwnedException();
        }
        return resolveContent(bookId);
    }

    // Same file resolution as getContent(), but for trusted internal callers (ai-service's reader pipeline) that already verified ownership themselves via isOwned() — skips the check since there's no end-user request context here.
    @Transactional(readOnly = true)
    public Resource getContentForInternalUse(UUID bookId) {
        return resolveContent(bookId);
    }

    // Whether a user owns an active, purchased virtual edition of a book.
    @Transactional(readOnly = true)
    public boolean isOwned(UUID userId, UUID bookId) {
        boolean hasActiveEdition = virtualEditionRepository.findById(bookId).filter(VirtualEdition::isActive).isPresent();
        return hasActiveEdition && commerceClient.getPurchasedBookIds(userId).contains(bookId);
    }

    // Resolves a book's active virtual edition to its readable file content.
    private Resource resolveContent(UUID bookId) {
        VirtualEdition edition = virtualEditionRepository.findById(bookId)
                .filter(VirtualEdition::isActive)
                .orElseThrow(VirtualEditionNotFoundException::new);

        return contentStore.resolve(edition.getFileUrl());
    }
}
