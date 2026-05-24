package com.company.newsservice.ingestion.infrastructure.rss;

import com.company.newsservice.image.infrastructure.normalizer.ArticleImageUrlNormalizer;
import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.modules.mediarss.types.Reference;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RSS entry metadata'sından (Media RSS, enclosure, HTML açıklama) hero görsel URL'sini çözümler.
 */
@Component
@RequiredArgsConstructor
public class RssEntryImageExtractor {

    private final RssDescriptionImageExtractor rssDescriptionImageExtractor;

    /**
     * Verilen RSS entry'sinden makale görsel URL'sini çıkarır.
     *
     * @param entry RSS entry
     * @param articleUrl göreli URL'ler için baz makale URL'si
     * @return normalize edilmiş görsel URL'si veya bulunamazsa {@code null}
     */
    public String extract(SyndEntry entry, String articleUrl) {
        if (entry == null) {
            return null;
        }
        String fromMedia = extractFromMediaModule(entry, articleUrl);
        if (fromMedia != null) {
            return fromMedia;
        }
        String fromEnclosure = extractFromEnclosures(entry, articleUrl);
        if (fromEnclosure != null) {
            return fromEnclosure;
        }
        String summary = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        return rssDescriptionImageExtractor.extract(summary, articleUrl);
    }

    private static String extractFromMediaModule(SyndEntry entry, String articleUrl) {
        Module module = entry.getModule(MediaModule.URI);
        if (!(module instanceof MediaEntryModule mediaEntry)) {
            return null;
        }
        String fromContents = firstFromMediaContents(mediaEntry.getMediaContents(), articleUrl);
        if (fromContents != null) {
            return fromContents;
        }
        return firstFromMediaGroups(mediaEntry.getMediaGroups(), articleUrl);
    }

    private static String firstFromMediaContents(MediaContent[] contents, String articleUrl) {
        if (contents == null) {
            return null;
        }
        for (MediaContent content : contents) {
            String url = resolveMediaReference(content != null ? content.getReference() : null, articleUrl);
            if (url != null) {
                return url;
            }
            if (content != null && content.getMetadata() != null) {
                String thumb = firstThumbnail(content.getMetadata(), articleUrl);
                if (thumb != null) {
                    return thumb;
                }
            }
        }
        return null;
    }

    private static String firstFromMediaGroups(MediaGroup[] groups, String articleUrl) {
        if (groups == null) {
            return null;
        }
        for (MediaGroup group : groups) {
            if (group == null) {
                continue;
            }
            String fromContents = firstFromMediaContents(group.getContents(), articleUrl);
            if (fromContents != null) {
                return fromContents;
            }
            if (group.getMetadata() != null) {
                String thumb = firstThumbnail(group.getMetadata(), articleUrl);
                if (thumb != null) {
                    return thumb;
                }
            }
        }
        return null;
    }

    private static String firstThumbnail(Metadata metadata, String articleUrl) {
        Thumbnail[] thumbs = metadata.getThumbnail();
        if (thumbs == null) {
            return null;
        }
        for (Thumbnail thumb : thumbs) {
            if (thumb == null || thumb.getUrl() == null) {
                continue;
            }
            String url = ArticleImageUrlNormalizer.resolve(thumb.getUrl().toString(), articleUrl);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static String resolveMediaReference(Reference reference, String articleUrl) {
        if (reference == null) {
            return null;
        }
        return ArticleImageUrlNormalizer.resolve(reference.toString(), articleUrl);
    }

    private static String extractFromEnclosures(SyndEntry entry, String articleUrl) {
        if (entry.getEnclosures() == null) {
            return null;
        }
        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            if (enclosure == null) {
                continue;
            }
            String type = enclosure.getType();
            if (type != null && !type.toLowerCase().startsWith("image/")) {
                continue;
            }
            String resolved = ArticleImageUrlNormalizer.resolve(enclosure.getUrl(), articleUrl);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }
}
