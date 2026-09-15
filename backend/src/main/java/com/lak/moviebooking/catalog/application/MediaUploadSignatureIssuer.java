package com.lak.moviebooking.catalog.application;

import java.util.UUID;

public interface MediaUploadSignatureIssuer {

    MediaUploadSignature issue(UUID actorId, MediaUploadRequest request);
}
