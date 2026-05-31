package com.ecodatahub.news.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String content;

    private LocalDate publicationDate;

    private String category;

    private String source;

    @Column(unique = true)
    private String sourceArticleId;

    private String sourceUrl;

}
