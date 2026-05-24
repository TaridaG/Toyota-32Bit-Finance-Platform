package com.company.newsservice.ingestion.infrastructure.rss;

import com.company.newsservice.image.infrastructure.rss.RssDescriptionImageExtractor;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RssEntryImageExtractorTest {

    private final RssEntryImageExtractor extractor =
            new RssEntryImageExtractor(new RssDescriptionImageExtractor());

    @Test
    void extractsBloombergMediaContentUrl() throws Exception {
        String rss = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
                  <channel>
                    <title>Bloomberg Markets</title>
                    <item>
                      <title>Sample</title>
                      <link>https://www.bloomberg.com/news/articles/2026-05-22/sample</link>
                      <description>Plain text summary without img tag.</description>
                      <media:content url="https://assets.bwbx.io/images/users/iqjWHBFdfxIU/ieL1WKAhA.eI/v1/1200x-1.jpg" type="image/jpeg"/>
                    </item>
                  </channel>
                </rss>
                """;

        SyndFeed feed = new SyndFeedInput().build(new StringReader(rss));
        SyndEntry entry = feed.getEntries().getFirst();
        String articleUrl = entry.getLink();

        String imageUrl = extractor.extract(entry, articleUrl);

        assertNotNull(imageUrl);
        assertEquals(
                "https://assets.bwbx.io/images/users/iqjWHBFdfxIU/ieL1WKAhA.eI/v1/1200x-1.jpg",
                imageUrl
        );
    }
}
