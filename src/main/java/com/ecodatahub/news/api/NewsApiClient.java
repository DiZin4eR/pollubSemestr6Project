package com.ecodatahub.news.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.ecodatahub.news.api.dto.NewsApiArticleDto;
import com.ecodatahub.news.api.dto.NewsApiResponse;

import java.util.List;

@Service
public class NewsApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final int pageSize;
    private final String country;

    public NewsApiClient(
            @Qualifier("newsApiWebClient") WebClient webClient,
            @Value("${news-api.api-key:${NEWS_API_KEY:}}") String apiKey,
            @Value("${news-api.page-size:20}") int pageSize,
            @Value("${news-api.country:us}") String country
    ) {
        this.webClient = webClient;
        this.apiKey = apiKey;
        this.pageSize = pageSize;
        this.country = country;
    }

    public List<NewsApiArticleDto> getBusinessArticles() {
        NewsApiResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/top-headlines")
                        .queryParam("country", country)
                        .queryParam("category", "business")
                        .queryParam("pageSize", pageSize)
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(NewsApiResponse.class)
                .block();

        return response != null && response.getArticles() != null ? response.getArticles() : List.of();
    }

    public List<NewsApiArticleDto> getEconomyArticles() {
        NewsApiResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/everything")
                        .queryParam("q", "economy")
                        .queryParam("language", "en")
                        .queryParam("sortBy", "publishedAt")
                        .queryParam("pageSize", pageSize)
                        .queryParam("apiKey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(NewsApiResponse.class)
                .block();

        return response != null && response.getArticles() != null ? response.getArticles() : List.of();
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
