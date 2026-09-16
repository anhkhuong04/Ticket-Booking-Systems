package com.lak.moviebooking.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = List.copyOf(allowedOrigins);
		if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> !validOrigin(origin))) {
			throw new IllegalArgumentException("app.cors.allowed-origins must contain explicit HTTP(S) origins only");
		}
	}

	private static boolean validOrigin(String origin) {
		try {
			java.net.URI uri = java.net.URI.create(origin);
			return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
					&& uri.getHost() != null
					&& (uri.getPath() == null || uri.getPath().isEmpty())
					&& uri.getQuery() == null
					&& uri.getFragment() == null
					&& !origin.contains("*");
		}
		catch (IllegalArgumentException exception) {
			return false;
		}
	}
}
