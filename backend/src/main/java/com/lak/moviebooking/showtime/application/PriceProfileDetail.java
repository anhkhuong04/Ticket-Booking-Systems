package com.lak.moviebooking.showtime.application;

import java.util.List;

public record PriceProfileDetail(PriceProfileView profile, List<PriceRuleView> rules) {
    public PriceProfileDetail {
        rules = List.copyOf(rules);
    }
}
