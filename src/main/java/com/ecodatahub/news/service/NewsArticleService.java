package com.ecodatahub.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.news.api.NewsApiClient;
import com.ecodatahub.news.api.dto.NewsApiArticleDto;
import com.ecodatahub.news.repository.NewsArticleRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleService {

    private static final String BUSINESS_CATEGORY = "business";
    private static final String ECONOMY_CATEGORY = "economy";
    private static final List<String> ALLOWED_CATEGORIES = List.of(BUSINESS_CATEGORY, ECONOMY_CATEGORY);
    private static final Duration REFRESH_TTL = Duration.ofMinutes(30);

    private final NewsApiClient newsApiClient;
    private final NewsArticleRepository newsArticleRepository;
    private Instant lastRefreshAt;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<NewsArticle> refreshAndGetArticles() {
        return refreshAndGetArticles(null, "all", "newest");
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<NewsArticle> refreshAndGetArticles(String query, String category, String sort) {
        refreshNewsIfStale();

        return newsArticleRepository.findByCategoryInOrderByPublicationDateDesc(ALLOWED_CATEGORIES).stream()
                .filter(article -> matchesCategory(article, category))
                .filter(article -> matchesQuery(article, query))
                .sorted(articleComparator(sort))
                .toList();
    }

    @Scheduled(
            initialDelayString = "${news-api.refresh-initial-delay-ms:30000}",
            fixedDelayString = "${news-api.refresh-delay-ms:1800000}"
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void refreshNews() {
        if (!newsApiClient.hasApiKey()) {
            log.warn("NEWS_API_KEY is not configured. Showing stored news only.");

            return;
        }

        try {
            saveArticles(newsApiClient.getBusinessArticles(), BUSINESS_CATEGORY);
            saveArticles(newsApiClient.getEconomyArticles(), ECONOMY_CATEGORY);
        } catch (WebClientResponseException exception) {
            log.warn(
                    "Could not refresh NewsAPI articles. Status: {}. Showing stored news only.",
                    exception.getStatusCode()
            );
        } catch (RuntimeException exception) {
            log.warn("Could not refresh NewsAPI articles. Showing stored news only.", exception);
        }
    }

    private void refreshNewsIfStale() {
        if (lastRefreshAt == null || Instant.now().isAfter(lastRefreshAt.plus(REFRESH_TTL))) {
            refreshNews();
            lastRefreshAt = Instant.now();
        }
    }

    private void saveArticles(List<NewsApiArticleDto> articles, String category) {
        if (articles == null) {
            return;
        }

        articles.stream()
                .filter(article -> article.getUrl() != null && !article.getUrl().isBlank())
                .filter(article -> !newsArticleRepository.existsBySourceArticleId(article.getUrl()))
                .map(article -> toNewsArticle(article, category))
                .forEach(newsArticleRepository::save);
    }

    private NewsArticle toNewsArticle(NewsApiArticleDto dto, String category) {
        NewsArticle article = new NewsArticle();

        article.setTitle(dto.getTitle());
        article.setContent(getArticleContent(dto));
        article.setPublicationDate(parsePublicationDate(dto.getPublishedAt()));
        article.setCategory(category);
        article.setSource(dto.getSource() != null ? dto.getSource().getName() : "NewsAPI");
        article.setSourceArticleId(dto.getUrl());
        article.setSourceUrl(dto.getUrl());

        return article;
    }

    private LocalDate parsePublicationDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException exception) {
            log.warn("Could not parse NewsAPI article date: {}", value);

            return null;
        }
    }

    private String getArticleContent(NewsApiArticleDto dto) {
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            return dto.getDescription();
        }

        return dto.getContent();
    }

    private boolean matchesCategory(NewsArticle article, String category) {
        if (category == null || category.isBlank() || "all".equals(category)) {
            return true;
        }

        return category.equals(article.getCategory());
    }

    private boolean matchesQuery(NewsArticle article, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);

        return contains(article.getTitle(), normalizedQuery)
                || contains(article.getContent(), normalizedQuery)
                || contains(article.getSource(), normalizedQuery);
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private Comparator<NewsArticle> articleComparator(String sort) {
        return switch (sort) {
            case "oldest" -> Comparator.comparing(
                    NewsArticle::getPublicationDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "title" -> Comparator.comparing(
                    NewsArticle::getTitle,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            case "source" -> Comparator.comparing(
                    NewsArticle::getSource,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            default -> Comparator.comparing(
                    NewsArticle::getPublicationDate,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        };
    }
}
