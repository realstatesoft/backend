package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.enums.ScreeningProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreeningAdapterFactoryTest {

    private final InternalScreeningAdapter internal = new InternalScreeningAdapter();
    private final TransUnionScreeningAdapter transUnion = new TransUnionScreeningAdapter();

    @Test
    void resolve_returnsCorrectAdapterPerEnum() {
        ScreeningAdapterFactory factory = new ScreeningAdapterFactory(
                List.of(internal, transUnion), ScreeningProvider.INTERNAL);

        assertThat(factory.resolve(ScreeningProvider.INTERNAL)).isSameAs(internal);
        assertThat(factory.resolve(ScreeningProvider.TRANSUNION)).isSameAs(transUnion);
    }

    @Test
    void resolveDefault_returnsConfiguredProvider() {
        ScreeningAdapterFactory factory = new ScreeningAdapterFactory(
                List.of(internal, transUnion), ScreeningProvider.TRANSUNION);

        assertThat(factory.resolveDefault()).isSameAs(transUnion);
        assertThat(factory.getDefaultProvider()).isEqualTo(ScreeningProvider.TRANSUNION);
    }

    @Test
    void resolve_unknownProvider_throwsBadRequest() {
        ScreeningAdapterFactory factory = new ScreeningAdapterFactory(
                List.of(internal), ScreeningProvider.INTERNAL);

        assertThatThrownBy(() -> factory.resolve(ScreeningProvider.EXPERIAN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("EXPERIAN");
    }
}
