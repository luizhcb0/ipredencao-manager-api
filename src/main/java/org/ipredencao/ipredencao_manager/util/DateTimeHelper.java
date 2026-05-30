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

    public static LocalDate toDbDate(org.joda.time.LocalDate localDate) {
        if (localDate == null) return null;
        return LocalDate.of(localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth());
    }

    public static org.joda.time.LocalDate fromDbDate(LocalDate localDate) {
        if (localDate == null) return null;
        return new org.joda.time.LocalDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    }
}
