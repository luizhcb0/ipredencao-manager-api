package org.ipredencao.ipredencao_manager.util;

import org.joda.time.DateTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeHelper {
    public static LocalDateTime toDb(DateTime dateTime) {
        if (dateTime == null) return null;
        return Instant.ofEpochMilli(dateTime.getMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static DateTime fromDb(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return new DateTime(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    public static LocalDate toDbDate(DateTime dateTime) {
        if (dateTime == null) return null;
        return LocalDate.of(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    public static DateTime fromDbDate(LocalDate localDate) {
        if (localDate == null) return null;
        return new DateTime(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), 0, 0);
    }
}
