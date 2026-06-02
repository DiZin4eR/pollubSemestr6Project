package com.ecodatahub.news.service;

import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.news.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsArticleCrudServiceTest {

    @Mock
    private NewsArticleRepository repository;

    @InjectMocks
    private NewsArticleCrudService service;

    @Test
    void createNewsArticleClearsIdBeforeSaving() {
        NewsArticle article = article(10L);
        when(repository.save(article)).thenReturn(article);

        NewsArticle saved = service.createNewsArticle(article);

        assertThat(saved).isSameAs(article);
        assertThat(article.getId()).isNull();
        verify(repository).save(article);
    }

    @Test
    void updateNewsArticleSavesWithRequestedIdWhenEntityExists() {
        NewsArticle existing = article(1L);
        NewsArticle update = article(null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(update)).thenReturn(update);

        Optional<NewsArticle> result = service.updateNewsArticle(1L, update);

        assertThat(result).containsSame(update);
        assertThat(update.getId()).isEqualTo(1L);
        verify(repository).save(update);
    }

    @Test
    void updateNewsArticleReturnsEmptyWhenEntityDoesNotExist() {
        NewsArticle update = article(null);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<NewsArticle> result = service.updateNewsArticle(99L, update);

        assertThat(result).isEmpty();
        verify(repository, never()).save(update);
    }

    @Test
    void deleteNewsArticleDeletesAndReportsTrueWhenEntityExists() {
        NewsArticle existing = article(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        boolean deleted = service.deleteNewsArticle(1L);

        assertThat(deleted).isTrue();
        verify(repository).delete(existing);
    }

    @Test
    void deleteNewsArticleReportsFalseWhenEntityDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        boolean deleted = service.deleteNewsArticle(99L);

        assertThat(deleted).isFalse();
        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private NewsArticle article(Long id) {
        NewsArticle article = new NewsArticle();
        article.setId(id);
        article.setTitle("Title");
        return article;
    }
}
