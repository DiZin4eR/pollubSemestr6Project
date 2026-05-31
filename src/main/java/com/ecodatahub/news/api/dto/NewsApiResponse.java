package com.ecodatahub.news.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class NewsApiResponse {

    private String status;
    private Integer totalResults;
    private String code;
    private String message;
    private List<NewsApiArticleDto> articles;
}
