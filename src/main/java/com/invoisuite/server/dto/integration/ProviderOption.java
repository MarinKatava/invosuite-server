package com.invoisuite.server.dto.integration;

import com.invoisuite.server.domain.IntegrationProvider;
import com.invoisuite.server.domain.OdooApiMode;

import java.util.List;

public record ProviderOption(String code, String label, List<OdooApiMode> apiModes) {
    public static ProviderOption from(IntegrationProvider provider) {
        return new ProviderOption(provider.name(), provider.label(), List.of(OdooApiMode.values()));
    }
}
