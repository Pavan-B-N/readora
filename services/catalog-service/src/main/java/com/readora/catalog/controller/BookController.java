package com.readora.catalog.controller;

import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSuggestionResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.PurchasedBookResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.security.CurrentUserContext;
import com.readora.catalog.service.CatalogService;
import com.readora.catalog.service.VirtualContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Books")
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final CatalogService catalogService;
    private final VirtualContentService virtualContentService;

    public BookController(CatalogService catalogService, VirtualContentService virtualContentService) {
        this.catalogService = catalogService;
        this.virtualContentService = virtualContentService;
    }

    @Operation(
            summary = "Search the catalogue",
            description = "Searches and filters books by free-text query, category, publisher, and price range. virtualOnly omitted (default) is the unified storefront view — physical books at storeId plus store-independent virtual editions, together; virtualOnly=false restricts to physical-at-storeId only; virtualOnly=true restricts to virtual editions only, ignoring store (and storeId) entirely. storeId is required unless virtualOnly=true. For a signed-in caller, virtual-only books they already own are excluded — there's nothing left to sell them there; see the \"Your orders\" rail for those. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated book results returned"),
            @ApiResponse(responseCode = "400", description = "storeId missing while a physical result could appear")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean virtualOnly,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        UUID userId = CurrentUserContext.get().orElse(null);
        return ResponseEntity.ok(catalogService.search(q, categoryId, publisherId, authorId, minPrice, maxPrice, virtualOnly, storeId, userId, pageable));
    }

    @Operation(
            summary = "The caller's most recent order line items",
            description = "Backs the \"Your orders\" rail — newest first, each paired with its order's status (cancelled/returned included, not filtered out). Empty for anonymous callers or callers with no orders — never an error.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent order items returned (possibly empty)")
    })
    @GetMapping("/purchased")
    public ResponseEntity<List<PurchasedBookResponse>> purchased() {
        UUID userId = CurrentUserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getPurchasedBooks(userId));
    }

    @Operation(
            summary = "The caller's readable virtual editions",
            description = "Backs the \"My library\" page — every virtual edition the caller owns and can open in the in-app reader (unlike /purchased, this excludes physical-only purchases and any virtual edition since deactivated). Empty for anonymous callers or callers with no owned virtual editions — never an error.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned virtual editions returned (possibly empty)")
    })
    @GetMapping("/library")
    public ResponseEntity<List<BookSummaryResponse>> library() {
        UUID userId = CurrentUserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getLibrary(userId));
    }

    @Operation(
            summary = "Look up books by id",
            description = "Batch lookup for an arbitrary set of book ids, e.g. to render a wishlist — deliberately unscoped by store, so a saved item still shows even when it isn't stocked at the caller's current store. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching books returned (possibly empty; unknown ids are silently skipped)")
    })
    @GetMapping("/batch")
    public ResponseEntity<List<BookSummaryResponse>> batch(@RequestParam List<UUID> ids) {
        return ResponseEntity.ok(catalogService.getBooksByIds(ids));
    }

    @Operation(
            summary = "Typeahead suggestions for the search bar",
            description = "Top title matches for a partial query, capped at 10, scoped to what's available to the caller (their store, or store-independent virtual editions). Public — no authentication required. Deliberately a plain substring match, not ai-service's semantic search — see CatalogService.suggest's javadoc for why.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestions returned (possibly empty)")
    })
    @GetMapping("/suggest")
    public ResponseEntity<List<BookSuggestionResponse>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(required = false) UUID storeId
    ) {
        return ResponseEntity.ok(catalogService.suggest(q, limit, storeId));
    }

    @Operation(
            summary = "Get personalized recommendations",
            description = "Books from the same categories as the caller's past purchases, excluding titles already owned, scoped to what's available to the caller (their store, or store-independent virtual editions). Empty for anonymous callers or callers with no purchase history — never an error.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommended books returned (possibly empty)")
    })
    @GetMapping("/recommended")
    public ResponseEntity<List<BookSummaryResponse>> recommended(@RequestParam(required = false) UUID storeId) {
        UUID userId = CurrentUserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getRecommendations(userId, storeId));
    }

    @Operation(
            summary = "Get book detail",
            description = "Returns full detail for one book, including live availability. Pass storeId (the caller's currently-delivering-from store) so a physical book stocked elsewhere correctly reports NOT_AVAILABLE_AT_STORE instead of its raw inventory count. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book detail returned"),
            @ApiResponse(responseCode = "404", description = "No active book with that id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getDetail(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(catalogService.getDetail(id, storeId));
    }

    @Operation(
            summary = "Get related titles",
            description = "Returns cross-sell titles related to one book. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Related titles returned"),
            @ApiResponse(responseCode = "404", description = "The parent book does not exist")
    })
    @GetMapping("/{id}/related")
    public ResponseEntity<List<RelatedBookResponse>> getRelated(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getRelated(id));
    }

    @Operation(
            summary = "Check whether an ISBN is already in use",
            description = "Lets the admin book form validate ISBN uniqueness live, before submitting. Public — no authentication required, same as the rest of this read-only surface.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "true if a book with this ISBN already exists")
    })
    @GetMapping("/check-isbn")
    public ResponseEntity<Boolean> checkIsbn(@RequestParam String isbn) {
        return ResponseEntity.ok(catalogService.existsByIsbn13(isbn));
    }

    @Operation(
            summary = "Read a virtual edition in-app",
            description = "Streams the virtual edition's file for in-app viewing only — never a downloadable link. Requires the caller to have purchased this book.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream begins"),
            @ApiResponse(responseCode = "403", description = "The caller hasn't purchased this book"),
            @ApiResponse(responseCode = "404", description = "No active virtual edition for this book")
    })
    @GetMapping("/{id}/read")
    public ResponseEntity<Resource> read(@PathVariable UUID id) {
        Resource content = virtualContentService.getContent(CurrentUserContext.require(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(content);
    }
}
