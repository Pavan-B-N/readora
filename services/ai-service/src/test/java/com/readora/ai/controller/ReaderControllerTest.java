package com.readora.ai.controller;

import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.BookContentIndexService;
import com.readora.ai.service.BookReaderChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReaderControllerTest {

    @Mock private BookContentIndexService indexService;
    @Mock private BookReaderChatService chatService;

    private ReaderController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ReaderController(indexService, chatService);
        CurrentUserContext.set(userId, List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void status_returnsWhateverTheIndexServiceReports() {
        when(indexService.getStatus(bookId)).thenReturn(BookReaderIndexStatus.READY);

        var response = controller.status(bookId);

        assertThat(response.getBody().status()).isEqualTo(BookReaderIndexStatus.READY);
    }

    @Test
    void initialize_delegatesToIndexServiceWithTheCurrentUser() {
        when(indexService.initialize(userId, bookId)).thenReturn(BookReaderIndexStatus.READY);

        var response = controller.initialize(bookId);

        assertThat(response.getBody().status()).isEqualTo(BookReaderIndexStatus.READY);
    }

    @Test
    void messages_delegatesToChatServiceWithTheCurrentUser() {
        BookReaderChatService.ReaderMessage msg = new BookReaderChatService.ReaderMessage("USER", "hi");
        when(chatService.getHistory(userId, bookId)).thenReturn(List.of(msg));

        var response = controller.messages(bookId);

        assertThat(response.getBody()).containsExactly(msg);
    }

    @Test
    void chat_delegatesToChatServiceAndWrapsTheReply() {
        when(chatService.chat(userId, bookId, "what happens next?")).thenReturn("the hero wins");

        var response = controller.chat(bookId, new ReaderController.ChatRequest("what happens next?"));

        assertThat(response.getBody().reply()).isEqualTo("the hero wins");
    }
}
