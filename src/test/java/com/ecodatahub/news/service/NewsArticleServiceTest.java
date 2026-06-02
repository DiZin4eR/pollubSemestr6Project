package com.ecodatahub.news.service;

import com.ecodatahub.news.api.NewsApiClient;
import com.ecodatahub.news.api.dto.NewsApiArticleDto;
import com.ecodatahub.news.api.dto.NewsApiSourceDto;
import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.news.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsArticleServiceTest {

    @Mock
    private NewsApiClient newsApiClient;

    @Mock
    private NewsArticleRepository repository;

    @InjectMocks
    private NewsArticleService service;

    @Test
    void refreshNewsDoesNothingWhenApiKeyIsMissing() {
        when(newsApiClient.hasApiKey()).thenReturn(false);

        service.refreshNews();

        verify(newsApiClient, never()).getBusinessArticles();
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refreshNewsSavesNewBusinessAndEconomyArticles() {
        NewsApiArticleDto business = apiArticle("Business title", "Business description", "https://example.com/business", "2026-06-01T10:15:30Z", "Reuters");
        NewsApiArticleDto economy = apiArticle("Economy title", null, "https://example.com/economy", "2026-05-31T08:00:00Z", null);
        economy.setContent("Economy content");
        when(newsApiClient.hasApiKey()).thenReturn(true);
        when(newsApiClient.getBusinessArticles()).thenReturn(List.of(business));
        when(newsApiClient.getEconomyArticles()).thenReturn(List.of(economy));
        when(repository.existsBySourceArticleId("https://example.com/business")).thenReturn(false);
        when(repository.existsBySourceArticleId("https://example.com/economy")).thenReturn(false);

        service.refreshNews();

        ArgumentCaptor<NewsArticle> captor = ArgumentCaptor.forClass(NewsArticle.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NewsArticle::getTitle)
                .containsExactly("Business title", "Economy title");
        assertThat(captor.getAllValues())
                .extracting(NewsArticle::getCategory)
                .containsExactly("business", "economy");
        assertThat(captor.getAllValues().get(0).getContent()).isEqualTo("Business description");
        assertThat(captor.getAllValues().get(0).getPublicationDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(captor.getAllValues().get(0).getSource()).isEqualTo("Reuters");
        assertThat(captor.getAllValues().get(1).getContent()).isEqualTo("Economy content");
        assertThat(captor.getAllValues().get(1).getSource()).isEqualTo("NewsAPI");
    }

    @Test
    void refreshNewsSkipsDuplicateAndBlankUrlArticles() {
        NewsApiArticleDto duplicate = apiArticle("Duplicate", "description", "https://example.com/duplicate", "2026-06-01T10:15:30Z", "Source");
        NewsApiArticleDto blankUrl = apiArticle("Blank URL", "description", " ", "2026-06-01T10:15:30Z", "Source");
        when(newsApiClient.hasApiKey()).thenReturn(true);
        when(newsApiClient.getBusinessArticles()).thenReturn(List.of(duplicate, blankUrl));
        when(newsApiClient.getEconomyArticles()).thenReturn(null);
        when(repository.existsBySourceArticleId("https://example.com/duplicate")).thenReturn(true);

        service.refreshNews();

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refreshAndGetArticlesFiltersByCategoryAndQueryThenSortsByTitle() {
        when(newsApiClient.hasApiKey()).thenReturn(false);
        NewsArticle alpha = article("Alpha market", "Inflation is lower", "business", "Reuters", LocalDate.of(2026, 6, 1));
        NewsArticle beta = article("Beta report", "Inflation is higher", "business", "Bloomberg", LocalDate.of(2026, 5, 30));
        NewsArticle economy = article("Economy report", "Inflation data", "economy", "AP", LocalDate.of(2026, 6, 2));
        when(repository.findByCategoryInOrderByPublicationDateDesc(List.of("business", "economy")))
                .thenReturn(List.of(beta, economy, alpha));

        List<NewsArticle> result = service.refreshAndGetArticles("inflation", "business", "title");

        assertThat(result).containsExactly(alpha, beta);
    }

    @Test
    void refreshAndGetArticlesSortsNewestByDefaultWithNullDatesLast() {
        when(newsApiClient.hasApiKey()).thenReturn(false);
        NewsArticle newest = article("Newest", null, "business", "Source", LocalDate.of(2026, 6, 2));
        NewsArticle older = article("Older", null, "economy", "Source", LocalDate.of(2026, 5, 1));
        NewsArticle undated = article("Undated", null, "business", "Source", null);
        when(repository.findByCategoryInOrderByPublicationDateDesc(List.of("business", "economy")))
                .thenReturn(List.of(undated, older, newest));

        List<NewsArticle> result = service.refreshAndGetArticles(null, "all", "unknown");

        assertThat(result).containsExactly(newest, older, undated);
    }

    private NewsApiArticleDto apiArticle(String title, String description, String url, String publishedAt, String sourceName) {
        NewsApiArticleDto dto = new NewsApiArticleDto();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setUrl(url);
        dto.setPublishedAt(publishedAt);
        if (sourceName != null) {
            NewsApiSourceDto source = new NewsApiSourceDto();
            source.setName(sourceName);
            dto.setSource(source);
        }
        return dto;
    }

    private NewsArticle article(String title, String content, String category, String source, LocalDate publicationDate) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setContent(content);
        article.setCategory(category);
        article.setSource(source);
        article.setPublicationDate(publicationDate);
        return article;
    }
}
