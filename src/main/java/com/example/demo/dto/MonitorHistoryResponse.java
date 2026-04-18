package com.example.demo.dto;

import java.util.List;

public record MonitorHistoryResponse(
        DeviceResponse device,
        MonitorFiltersResponse filters,
        List<MonitorHistoryRowResponse> rows,
        MonitorSummaryResponse summary) {
}
