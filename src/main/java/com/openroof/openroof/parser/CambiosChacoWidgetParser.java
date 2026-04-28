package com.openroof.openroof.parser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.openroof.openroof.exception.ExchangeRateUnavailableException;

/**
 * Parser tolerante al HTML del widget público de Cambios Chaco.
 */
@Component
public class CambiosChacoWidgetParser {

    public static final String USD = "USD";
    public static final String BRL = "BRL";

    private static final Pattern LAST_UPDATED_PATTERN = Pattern.compile(
            "Última Actualización:\\s*<span class=\"time\">.*?(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ROW_PATTERN = Pattern.compile(
            "<tr>\\s*<td><i class=\"moneda\\s+([^\"]+)\"></i>\\s*([^<]+?)</td>\\s*<td class=\"text-right\">\\s*([\\d.,]+).*?</td>\\s*<td class=\"text-right\">\\s*([\\d.,]+).*?</td>\\s*</tr>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ParsedWidgetRates parse(String html) {
        if (!StringUtils.hasText(html)) {
            throw new ExchangeRateUnavailableException("El widget de cotizaciones llegó vacío");
        }

        String normalizedHtml = normalizeHtml(html);
        LocalDateTime sourceUpdatedAt = parseLastUpdated(normalizedHtml).orElse(null);

        Map<String, ParsedRate> rates = new LinkedHashMap<>();
        Matcher matcher = ROW_PATTERN.matcher(normalizedHtml);

        while (matcher.find()) {
            String iconClass = matcher.group(1);
            String label = normalizeText(matcher.group(2));
            BigDecimal buyRate = parseNumber(matcher.group(3));
            BigDecimal sellRate = parseNumber(matcher.group(4));

            detectCurrencyCode(iconClass, label).ifPresent(currencyCode -> {
                String currencyName = switch (currencyCode) {
                    case USD -> "Dólar Americano";
                    case BRL -> "Real";
                    default -> label;
                };
                rates.put(currencyCode, new ParsedRate(currencyCode, currencyName, buyRate, sellRate));
            });
        }

        if (!rates.containsKey(USD) || !rates.containsKey(BRL)) {
            throw new ExchangeRateUnavailableException("No se pudieron identificar las cotizaciones USD y BRL en el widget");
        }

        return new ParsedWidgetRates(rates, sourceUpdatedAt);
    }

    private Optional<LocalDateTime> parseLastUpdated(String html) {
        Matcher matcher = LAST_UPDATED_PATTERN.matcher(html);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDateTime.parse(matcher.group(1).trim(), UPDATED_AT_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new ExchangeRateUnavailableException("No se pudo interpretar la fecha de actualización del widget", ex);
        }
    }

    private Optional<String> detectCurrencyCode(String iconClass, String label) {
        String normalizedIcon = normalizeText(iconClass);
        String normalizedLabel = normalizeText(label);

        if (normalizedIcon.contains("dolarus") || normalizedLabel.contains("dólar americano")
                || normalizedLabel.contains("us dollar") || normalizedLabel.contains("dolar americano")) {
            return Optional.of(USD);
        }

        if (normalizedIcon.contains("real") || normalizedLabel.contains("brazilian real")
                || normalizedLabel.equals("real")) {
            return Optional.of(BRL);
        }

        return Optional.empty();
    }

    private BigDecimal parseNumber(String value) {
        String cleaned = value.trim().replace("\u00a0", "");
        String normalized = cleaned.contains(",")
                ? cleaned.replace(".", "").replace(",", ".")
                : cleaned.replace(".", "");
        return new BigDecimal(normalized);
    }

    private String normalizeHtml(String html) {
        return html.replace("&nbsp;", " ").replace('\u00a0', ' ');
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ParsedWidgetRates(Map<String, ParsedRate> rates, LocalDateTime sourceUpdatedAt) {
    }

    public record ParsedRate(String currencyCode, String currencyName, BigDecimal buyRate, BigDecimal sellRate) {
    }
}
