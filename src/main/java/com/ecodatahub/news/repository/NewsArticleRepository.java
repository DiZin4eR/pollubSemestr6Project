package com.ecodatahub.news.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ecodatahub.news.domain.NewsArticle;

import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsBySourceArticleId(String sourceArticleId);

    List<NewsArticle> findByCategoryInOrderByPublicationDateDesc(List<String> categories);
}
