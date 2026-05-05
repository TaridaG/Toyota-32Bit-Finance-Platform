package com.company.newsservice.repository;

import com.company.newsservice.domain.NewsArticleTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NewsArticleTranslationRepository extends JpaRepository<NewsArticleTranslation, Long> {

    Optional<NewsArticleTranslation> findByNewsArticleIdAndLanguageCode(Long newsArticleId, String languageCode);

    List<NewsArticleTranslation> findByNewsArticleIdInAndLanguageCode(Collection<Long> newsArticleIds, String languageCode);
}
