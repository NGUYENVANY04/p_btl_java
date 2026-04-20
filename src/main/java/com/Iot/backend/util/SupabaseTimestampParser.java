package com.Iot.backend.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

public final class SupabaseTimestampParser {

    private SupabaseTimestampParser() {
    }

    public static OffsetDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime();
        }
    }
}
