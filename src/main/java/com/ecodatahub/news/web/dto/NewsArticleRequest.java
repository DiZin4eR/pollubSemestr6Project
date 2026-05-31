package com.ecodatahub.news.web.dto;

import com.ecodatahub.news.domain.NewsArticle;

import java.time.LocalDate;

public record NewsArticleRequest(
        String title,
        String content,
        LocalDate publicationDate,
        String category,
        String source,
        String sourceArticleId,
        String sourceUrl
) {

    public NewsArticle toEntity() {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setContent(content);
        article.setPublicationDate(publicationDate);
        article.setCategory(category);
        article.setSource(source);
        article.setSourceArticleId(sourceArticleId);
        article.setSourceUrl(sourceUrl);
        return article;
    }
}
