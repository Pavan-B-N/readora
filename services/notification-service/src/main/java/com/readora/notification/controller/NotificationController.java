package com.readora.notification.controller;

import com.readora.notification.dto.NotificationResponse;
import com.readora.notification.dto.UnreadCountResponse;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "List the caller's notifications",
            description = "Paginated, newest-first. Persisted history — includes notifications from before the caller was connected to the live WebSocket feed.",
            tags = {"Notifications"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification page returned")
    })
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.list(CurrentUserContext.require(), pageable));
    }

    @Operation(summary = "Get the caller's unread notification count", tags = {"Notifications"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count returned")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCount(CurrentUserContext.require())));
    }

    @Operation(summary = "Mark one notification read", tags = {"Notifications"})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marked read"),
            @ApiResponse(responseCode = "404", description = "No such notification belonging to the caller")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(CurrentUserContext.require(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all of the caller's notifications read", tags = {"Notifications"})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All marked read")
    })
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(CurrentUserContext.require());
        return ResponseEntity.noContent().build();
    }
}
