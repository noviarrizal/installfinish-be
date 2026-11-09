package com.installfinish.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param allowedOrigins exact browser origins (e.g. the installfinish-fe deployment) allowed to call the API */
@ConfigurationProperties("installfinish.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::strip).filter(origin -> !origin.isEmpty()).toList();
    }
}
