package com.openroof.openroof.parser;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})");

    private static final Pattern ROW_PATTERN = Pattern.compile("<tr[^>]*>(.*?)</tr>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<td(?:\\s+[^>]*)?>(.*?)</td>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ICON_CLASS_PATTERN = Pattern.compile("class=\"moneda\\s+([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);

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
            List<String> cells = extractCells(matcher.group(1));
            if (cells.size() < 3) {
                continue;
            }

            String iconClass = extractIconClass(cells.get(0));
            String label = normalizeText(stripTags(cells.get(0)));
            BigDecimal buyRate = parseNumber(stripTags(cells.get(1)));
            BigDecimal sellRate = parseNumber(stripTags(cells.get(2)));

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
        int index = html.indexOf("Última Actualización:");
        if (index == -1) {
            index = html.toLowerCase(Locale.ROOT).indexOf("última actualización:");
        }
        if (index == -1) {
            return Optional.empty();
        }

        int start = index + "Última Actualización:".length();
        int end = Math.min(start + 150, html.length());
        String window = html.substring(start, end);

        Matcher matcher = DATE_PATTERN.matcher(window);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDateTime.parse(matcher.group(1).trim(), UPDATED_AT_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new ExchangeRateUnavailableException("No se pudo interpretar la fecha de actualización del widget", ex);
        }
    }

    private List<String> extractCells(String rowHtml) {
        List<String> cells = new ArrayList<>();
        Matcher matcher = CELL_PATTERN.matcher(rowHtml);
        while (matcher.find()) {
            cells.add(matcher.group(1));
        }
        return cells;
    }

    private String extractIconClass(String cellHtml) {
        Matcher matcher = ICON_CLASS_PATTERN.matcher(cellHtml);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String stripTags(String html) {
        StringBuilder text = new StringBuilder(html.length());
        boolean insideTag = false;
        for (int i = 0; i < html.length(); i++) {
            char current = html.charAt(i);
            if (current == '<') {
                insideTag = true;
            } else if (current == '>') {
                insideTag = false;
            } else if (!insideTag) {
                text.append(current);
            }
        }
        return text.toString();
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
