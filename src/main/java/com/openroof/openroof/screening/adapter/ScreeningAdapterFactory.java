package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.ScreeningProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ScreeningAdapterFactory {

    private final Map<ScreeningProvider, ScreeningProviderAdapter> adapters;
    private final ScreeningProvider defaultProvider;

    public ScreeningAdapterFactory(
            List<ScreeningProviderAdapter> adapterList,
            @Value("${screening.provider:INTERNAL}") ScreeningProvider defaultProvider) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        ScreeningProviderAdapter::provider,
                        Function.identity()));
        this.defaultProvider = defaultProvider;
    }

    public ScreeningProviderAdapter resolve(ScreeningProvider provider) {
        ScreeningProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new BadRequestException("No adapter registered for screening provider " + provider);
        }
        return adapter;
    }

    public ScreeningProviderAdapter resolveDefault() {
        return resolve(defaultProvider);
    }

    public ScreeningProvider getDefaultProvider() {
        return defaultProvider;
    }
}
