package com.ecodatahub.news.service;

import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class NewsArticleCrudService {

    private final NewsArticleRepository newsArticleRepository;

    public List<NewsArticle> getNewsArticles() {
        return newsArticleRepository.findAll();
    }

    public Optional<NewsArticle> getNewsArticle(Long id) {
        return newsArticleRepository.findById(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public NewsArticle createNewsArticle(NewsArticle article) {
        article.setId(null);

        return newsArticleRepository.save(article);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<NewsArticle> updateNewsArticle(Long id, NewsArticle article) {
        if (newsArticleRepository.findById(id).isEmpty()) {
            return Optional.empty();
        }

        article.setId(id);

        return Optional.of(newsArticleRepository.save(article));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean deleteNewsArticle(Long id) {
        return newsArticleRepository.findById(id)
                .map(article -> {
                    newsArticleRepository.delete(article);
                    return true;
                })
                .orElse(false);
    }
}
