package com.ecodatahub.news.web.dto;

import com.ecodatahub.news.domain.NewsArticle;

import java.time.LocalDate;

public record NewsArticleResponse(
        Long id,
        String title,
        String content,
        LocalDate publicationDate,
        String category,
        String source,
        String sourceArticleId,
        String sourceUrl
) {

    public static NewsArticleResponse from(NewsArticle article) {
        return new NewsArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getPublicationDate(),
                article.getCategory(),
                article.getSource(),
                article.getSourceArticleId(),
                article.getSourceUrl()
        );
    }
}
