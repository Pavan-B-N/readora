package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.entity.VirtualEdition;
import com.readora.catalog.exception.VirtualEditionNotFoundException;
import com.readora.catalog.exception.VirtualEditionNotOwnedException;
import com.readora.catalog.repository.VirtualEditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirtualContentServiceTest {

    @Mock private VirtualEditionRepository virtualEditionRepository;
    @Mock private CommerceClient commerceClient;

    private VirtualContentService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VirtualContentService(virtualEditionRepository, commerceClient, System.getProperty("java.io.tmpdir"));
    }

    private VirtualEdition activeEdition(String fileUrl) {
        VirtualEdition edition = org.mockito.Mockito.mock(VirtualEdition.class);
        lenient().when(edition.isActive()).thenReturn(true);
        lenient().when(edition.getFileUrl()).thenReturn(fileUrl);
        return edition;
    }

    @Test
    void isOwned_noActiveEdition_isFalseWithoutCallingCommerce() {
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThat(service.isOwned(userId, bookId)).isFalse();
    }

    @Test
    void isOwned_activeEditionAndPurchased_isTrue() {
        VirtualEdition edition = activeEdition("file.pdf");
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(edition));
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(bookId));

        assertThat(service.isOwned(userId, bookId)).isTrue();
    }

    @Test
    void isOwned_activeEditionButNotPurchased_isFalse() {
        VirtualEdition edition = activeEdition("file.pdf");
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(edition));
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());

        assertThat(service.isOwned(userId, bookId)).isFalse();
    }

    @Test
    void getContent_notOwned_throws() {
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContent(userId, bookId)).isInstanceOf(VirtualEditionNotOwnedException.class);
    }

    @Test
    void getContent_owned_resolvesFileWithinStorageRoot() {
        VirtualEdition edition = activeEdition("book.pdf");
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(edition));
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(bookId));

        Resource resource = service.getContent(userId, bookId);

        assertThat(resource.getFilename()).isEqualTo("book.pdf");
    }

    @Test
    void getContent_pathTraversalAttempt_isRejectedAsNotFound() {
        VirtualEdition edition = activeEdition("../../etc/passwd");
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(edition));
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(bookId));

        assertThatThrownBy(() -> service.getContent(userId, bookId)).isInstanceOf(VirtualEditionNotFoundException.class);
    }

    @Test
    void getContentForInternalUse_skipsOwnershipCheck() {
        VirtualEdition edition = activeEdition("book.pdf");
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.of(edition));

        Resource resource = service.getContentForInternalUse(bookId);

        assertThat(resource.getFilename()).isEqualTo("book.pdf");
        org.mockito.Mockito.verifyNoInteractions(commerceClient);
    }

    @Test
    void getContentForInternalUse_noActiveEdition_throws() {
        when(virtualEditionRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getContentForInternalUse(bookId)).isInstanceOf(VirtualEditionNotFoundException.class);
    }
}
