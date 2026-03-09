package com.company.newsservice.config;

import com.company.newsservice.domain.enums.NewsCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "news")
public class NewsProperties {

    private Scheduler scheduler = new Scheduler();
    private List<Feed> feeds = new ArrayList<>();

    @Getter
    @Setter
    public static class Scheduler {
        private boolean enabled = true;
        private long delayMs = 300000; // 5 dk
    }

    @Getter
    @Setter
    public static class Feed {
        @NotBlank
        private String name;

        @NotBlank
        private String url;

        private NewsCategory category = NewsCategory.OTHER;
    }
}