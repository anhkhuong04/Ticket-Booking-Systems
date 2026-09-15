package com.lak.moviebooking.catalog.application;

public record MediaUploadSignature(String cloudName, String apiKey, String folder, long timestamp, String signature) {
}
