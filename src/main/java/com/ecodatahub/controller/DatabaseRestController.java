package com.ecodatahub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.ecodatahub.gus.service.GusDataAttributeService;
import com.ecodatahub.gus.service.GusImportStateService;
import com.ecodatahub.gus.service.GusSubjectCrudService;
import com.ecodatahub.gus.web.dto.GusDataAttributeRequest;
import com.ecodatahub.gus.web.dto.GusDataAttributeResponse;
import com.ecodatahub.gus.web.dto.GusImportStateRequest;
import com.ecodatahub.gus.web.dto.GusImportStateResponse;
import com.ecodatahub.gus.web.dto.GusSubjectRequest;
import com.ecodatahub.gus.web.dto.GusSubjectResponse;
import com.ecodatahub.news.service.NewsArticleCrudService;
import com.ecodatahub.news.web.dto.NewsArticleRequest;
import com.ecodatahub.news.web.dto.NewsArticleResponse;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Database CRUD", description = "CRUD endpoints for database-backed application resources")
@SecurityRequirement(name = "bearerAuth")
public class DatabaseRestController {

    private static final String READ_ACCESS = "Requires JWT authentication with role USER or ADMIN.";
    private static final String ADMIN_ACCESS = "Requires JWT authentication with role ADMIN.";

    private final NewsArticleCrudService newsArticleCrudService;
    private final GusSubjectCrudService gusSubjectCrudService;
    private final GusDataAttributeService gusDataAttributeService;
    private final GusImportStateService gusImportStateService;

    @GetMapping("/news-articles")
    @Operation(summary = "List news articles", description = READ_ACCESS)
    public List<NewsArticleResponse> getNewsArticles() {
        return newsArticleCrudService.getNewsArticles().stream()
                .map(NewsArticleResponse::from)
                .toList();
    }

    @GetMapping("/news-articles/{id}")
    @Operation(summary = "Get a news article by id", description = READ_ACCESS)
    public ResponseEntity<NewsArticleResponse> getNewsArticle(@PathVariable Long id) {
        return newsArticleCrudService.getNewsArticle(id)
                .map(NewsArticleResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/news-articles")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a news article", description = ADMIN_ACCESS)
    public NewsArticleResponse createNewsArticle(@RequestBody NewsArticleRequest article) {
        return NewsArticleResponse.from(newsArticleCrudService.createNewsArticle(article.toEntity()));
    }

    @PutMapping("/news-articles/{id}")
    @Operation(summary = "Update a news article", description = ADMIN_ACCESS)
    public ResponseEntity<NewsArticleResponse> updateNewsArticle(
            @PathVariable Long id,
            @RequestBody NewsArticleRequest article
    ) {
        return newsArticleCrudService.updateNewsArticle(id, article.toEntity())
                .map(NewsArticleResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/news-articles/{id}")
    @Operation(summary = "Delete a news article", description = ADMIN_ACCESS)
    public ResponseEntity<Void> deleteNewsArticle(@PathVariable Long id) {
        return newsArticleCrudService.deleteNewsArticle(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/gus-subjects")
    @Operation(summary = "List GUS subjects", description = READ_ACCESS)
    public List<GusSubjectResponse> getGusSubjects() {
        return gusSubjectCrudService.getGusSubjects().stream()
                .map(GusSubjectResponse::from)
                .toList();
    }

    @GetMapping("/gus-subjects/{id}")
    @Operation(summary = "Get a GUS subject by id", description = READ_ACCESS)
    public ResponseEntity<GusSubjectResponse> getGusSubject(@PathVariable Long id) {
        return gusSubjectCrudService.getGusSubject(id)
                .map(GusSubjectResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/gus-subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a GUS subject", description = ADMIN_ACCESS)
    public GusSubjectResponse createGusSubject(@RequestBody GusSubjectRequest subject) {
        return GusSubjectResponse.from(gusSubjectCrudService.createGusSubject(subject.toEntity()));
    }

    @PutMapping("/gus-subjects/{id}")
    @Operation(summary = "Update a GUS subject", description = ADMIN_ACCESS)
    public ResponseEntity<GusSubjectResponse> updateGusSubject(
            @PathVariable Long id,
            @RequestBody GusSubjectRequest subject
    ) {
        return gusSubjectCrudService.updateGusSubject(id, subject.toEntity())
                .map(GusSubjectResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/gus-subjects/{id}")
    @Operation(summary = "Delete a GUS subject", description = ADMIN_ACCESS)
    public ResponseEntity<Void> deleteGusSubject(@PathVariable Long id) {
        return gusSubjectCrudService.deleteGusSubject(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/gus-data-attributes")
    @Operation(summary = "List GUS data attributes", description = READ_ACCESS)
    public List<GusDataAttributeResponse> getGusDataAttributes() {
        return gusDataAttributeService.getGusDataAttributes().stream()
                .map(GusDataAttributeResponse::from)
                .toList();
    }

    @GetMapping("/gus-data-attributes/{id}")
    @Operation(summary = "Get a GUS data attribute by id", description = READ_ACCESS)
    public ResponseEntity<GusDataAttributeResponse> getGusDataAttribute(@PathVariable Integer id) {
        return gusDataAttributeService.getGusDataAttribute(id)
                .map(GusDataAttributeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/gus-data-attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a GUS data attribute", description = ADMIN_ACCESS)
    public GusDataAttributeResponse createGusDataAttribute(@RequestBody GusDataAttributeRequest attribute) {
        return GusDataAttributeResponse.from(gusDataAttributeService.createGusDataAttribute(attribute.toEntity()));
    }

    @PutMapping("/gus-data-attributes/{id}")
    @Operation(summary = "Update a GUS data attribute", description = ADMIN_ACCESS)
    public ResponseEntity<GusDataAttributeResponse> updateGusDataAttribute(
            @PathVariable Integer id,
            @RequestBody GusDataAttributeRequest attribute
    ) {
        return gusDataAttributeService.updateGusDataAttribute(id, attribute.toEntity())
                .map(GusDataAttributeResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/gus-data-attributes/{id}")
    @Operation(summary = "Delete a GUS data attribute", description = ADMIN_ACCESS)
    public ResponseEntity<Void> deleteGusDataAttribute(@PathVariable Integer id) {
        return gusDataAttributeService.deleteGusDataAttribute(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/gus-subject-import-states")
    @Operation(summary = "List GUS subject import states", description = READ_ACCESS)
    public List<GusImportStateResponse> getGusSubjectImportStates() {
        return gusImportStateService.getGusSubjectImportStates().stream()
                .map(GusImportStateResponse::from)
                .toList();
    }

    @GetMapping("/gus-subject-import-states/{id}")
    @Operation(summary = "Get a GUS subject import state by id", description = READ_ACCESS)
    public ResponseEntity<GusImportStateResponse> getGusSubjectImportState(@PathVariable Long id) {
        return gusImportStateService.getGusSubjectImportState(id)
                .map(GusImportStateResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/gus-subject-import-states")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a GUS subject import state", description = ADMIN_ACCESS)
    public GusImportStateResponse createGusSubjectImportState(@RequestBody GusImportStateRequest state) {
        return GusImportStateResponse.from(gusImportStateService.createGusSubjectImportState(state.toEntity()));
    }

    @PutMapping("/gus-subject-import-states/{id}")
    @Operation(summary = "Update a GUS subject import state", description = ADMIN_ACCESS)
    public ResponseEntity<GusImportStateResponse> updateGusSubjectImportState(
            @PathVariable Long id,
            @RequestBody GusImportStateRequest state
    ) {
        return gusImportStateService.updateGusSubjectImportState(id, state.toEntity())
                .map(GusImportStateResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/gus-subject-import-states/{id}")
    @Operation(summary = "Delete a GUS subject import state", description = ADMIN_ACCESS)
    public ResponseEntity<Void> deleteGusSubjectImportState(@PathVariable Long id) {
        return gusImportStateService.deleteGusSubjectImportState(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
