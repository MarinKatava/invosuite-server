package com.invoisuite.server.domain;

public enum Currency {
    EUR("€"),
    USD("$"),
    GBP("£"),
    CHF("CHF"),
    BAM("KM"),
    AUD("A$"),
    CAD("C$");

    private final String symbol;

    Currency(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
