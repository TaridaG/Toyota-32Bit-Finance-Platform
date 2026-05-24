package com.company.newsservice.translation.infrastructure.persistence;

import com.company.newsservice.translation.domain.NewsArticleTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link NewsArticleTranslation} için JPA erişimi.
 */
public interface NewsArticleTranslationRepository extends JpaRepository<NewsArticleTranslation, Long> {

    /** Makale ve dil kodu için tek çeviri satırını döner. */
    Optional<NewsArticleTranslation> findByNewsArticleIdAndLanguageCode(Long newsArticleId, String languageCode);

    /** Birden fazla makale için aynı dildeki çeviri satırlarını toplu döner. */
    List<NewsArticleTranslation> findByNewsArticleIdInAndLanguageCode(Collection<Long> newsArticleIds, String languageCode);
}
