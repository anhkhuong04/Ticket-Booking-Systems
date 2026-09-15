package com.lak.moviebooking.catalog.application;

public record MediaUploadRequest(String filename, String contentType, long sizeBytes) {
}
