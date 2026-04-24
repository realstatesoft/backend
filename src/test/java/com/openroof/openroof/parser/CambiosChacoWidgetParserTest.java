package com.openroof.openroof.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.openroof.openroof.exception.ExchangeRateUnavailableException;

class CambiosChacoWidgetParserTest {

    private final CambiosChacoWidgetParser parser = new CambiosChacoWidgetParser();

    @Test
    void parse_extractsUsdAndBrlRates() {
        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <body>
                <div class="cotiz-update clearfix">
                    Última Actualización: <span class="time"><i class="fa fa-clock-o"></i> 23/04/2026 17:01</span>
                </div>
                <table class="table table-hover cotiz-tabla">
                    <tbody>
                        <tr>
                            <td><i class="moneda dolarUs"></i> Dólar Americano</td>
                            <td class="text-right"> 6.300 <i class="estado suba"></i></td>
                            <td class="text-right"> 6.360 <i class="estado baja"></i></td>
                        </tr>
                        <tr>
                            <td><i class="moneda real"></i> Real</td>
                            <td class="text-right"> 1.210 <i class="estado suba"></i></td>
                            <td class="text-right"> 1.260 <i class="estado suba"></i></td>
                        </tr>
                    </tbody>
                </table>
                </body>
                </html>
                """;

        CambiosChacoWidgetParser.ParsedWidgetRates parsed = parser.parse(html);

        assertThat(parsed.sourceUpdatedAt()).isNotNull();
        assertThat(parsed.sourceUpdatedAt().toString()).startsWith("2026-04-23T17:01");
        assertThat(parsed.rates()).containsKeys(CambiosChacoWidgetParser.USD, CambiosChacoWidgetParser.BRL);
        assertThat(parsed.rates().get(CambiosChacoWidgetParser.USD).buyRate()).isEqualByComparingTo(new BigDecimal("6300"));
        assertThat(parsed.rates().get(CambiosChacoWidgetParser.USD).sellRate()).isEqualByComparingTo(new BigDecimal("6360"));
        assertThat(parsed.rates().get(CambiosChacoWidgetParser.BRL).buyRate()).isEqualByComparingTo(new BigDecimal("1210"));
        assertThat(parsed.rates().get(CambiosChacoWidgetParser.BRL).sellRate()).isEqualByComparingTo(new BigDecimal("1260"));
    }

    @Test
    void parse_emptyHtmlFailsSafely() {
        assertThatThrownBy(() -> parser.parse(""))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
