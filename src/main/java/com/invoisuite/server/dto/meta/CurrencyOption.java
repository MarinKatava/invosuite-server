package com.invoisuite.server.dto.meta;

import com.invoisuite.server.domain.Currency;

public record CurrencyOption(String code, String symbol) {
    public static CurrencyOption from(Currency currency) {
        return new CurrencyOption(currency.name(), currency.symbol());
    }
}
