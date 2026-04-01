package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.MonthlySalesData;
import com.openroof.openroof.dto.dashboard.RawSalesData;
import com.openroof.openroof.dto.dashboard.YearData;
import com.openroof.openroof.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ContractRepository contractRepository;

    public List<MonthlySalesData> getSalesPerformance(String agentEmail) {
        int currentYear = LocalDate.now().getYear();
        int previousYear = currentYear - 1;

        List<RawSalesData> rawData = contractRepository.getAgentPerformanceData(
                agentEmail, currentYear, previousYear);

        // Index raw data by "year-month" key for fast lookup
        Map<String, RawSalesData> dataMap = rawData.stream()
                .collect(Collectors.toMap(
                        r -> r.year() + "-" + r.month(),
                        r -> r
                ));

        List<MonthlySalesData> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            String currentKey = currentYear + "-" + month;
            String previousKey = previousYear + "-" + month;

            RawSalesData currentData = dataMap.get(currentKey);
            RawSalesData previousData = dataMap.get(previousKey);

            YearData currentYearData = currentData != null
                    ? new YearData(currentData.totalAmount(), currentData.count())
                    : YearData.zero();

            YearData previousYearData = previousData != null
                    ? new YearData(previousData.totalAmount(), previousData.count())
                    : YearData.zero();

            result.add(new MonthlySalesData(month, currentYearData, previousYearData));
        }

        return result;
    }
}
