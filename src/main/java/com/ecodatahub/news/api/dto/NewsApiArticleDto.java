package com.ecodatahub.news.api.dto;

import lombok.Data;

@Data
public class NewsApiArticleDto {

    private NewsApiSourceDto source;
    private String author;
    private String title;
    private String description;
    private String url;
    private String urlToImage;
    private String publishedAt;
    private String content;
}
