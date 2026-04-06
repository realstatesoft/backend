package com.openroof.openroof.dto.dashboard;

public record MonthlySalesData(
        int month,
        YearData currentYear,
        YearData previousYear
) {
}
