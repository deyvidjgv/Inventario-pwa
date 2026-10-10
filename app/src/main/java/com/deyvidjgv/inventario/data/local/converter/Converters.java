package com.deyvidjgv.inventario.data.local.converter;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDate;

public class Converters {

    @TypeConverter
    public static Instant fromTimestamp(Long value) {
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    @TypeConverter
    public static Long instantToTimestamp(Instant instant) {
        return instant == null ? null : instant.toEpochMilli();
    }

    @TypeConverter
    public static LocalDate fromDateString(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    @TypeConverter
    public static String localDateToString(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
