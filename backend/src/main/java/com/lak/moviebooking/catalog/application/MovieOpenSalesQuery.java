package com.lak.moviebooking.catalog.application;

import java.time.Instant;
import java.util.UUID;

public interface MovieOpenSalesQuery {
    boolean hasOpenSales(UUID movieId, Instant now);
}
