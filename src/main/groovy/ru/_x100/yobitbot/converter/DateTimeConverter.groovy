package ru._x100.yobitbot.converter

import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.sql.Timestamp
import java.time.LocalDateTime

@Converter(autoApply = true)
class DateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

  @Override
  Timestamp convertToDatabaseColumn(LocalDateTime localDateTime) {
     return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
   }

  @Override
  LocalDateTime convertToEntityAttribute(Timestamp timestamp) {
     return timestamp != null ? timestamp.toLocalDateTime() : null;
   }
}