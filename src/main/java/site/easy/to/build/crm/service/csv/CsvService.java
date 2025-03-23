package site.easy.to.build.crm.service.csv;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class CsvService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public <T> List<T> importCsv(String filePath, Class<T> clazz) throws Exception {
        List<T> resultList = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(Paths.get(filePath));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                T instance = clazz.getDeclaredConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String columnValue = record.get(field.getName());
                    if (columnValue != null) {
                        Object parsedValue = parseValue(field.getType(), columnValue);
                        field.set(instance, parsedValue);
                    }
                }
                resultList.add(instance);
            }
        }

        return resultList;
    }

    private Object parseValue(Class<?> fieldType, String value) {
        try {
            if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                return Integer.parseInt(value);
            } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                return Double.parseDouble(value);
            } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
                return Boolean.parseBoolean(value);
            } else if (fieldType.equals(String.class)) {
                return value;
            } else if (fieldType.isArray()) {
                return value.split(",");
            } else if (List.class.isAssignableFrom(fieldType)) {
                return Arrays.stream(value.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()); 
            } else if (fieldType.equals(LocalDateTime.class)) {
                return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            } else if (fieldType.equals(LocalTime.class)) {
                return LocalTime.parse(value, TIME_FORMATTER);
            } else if (fieldType.equals(LocalDate.class)) {
                return LocalDate.parse(value, DATE_FORMATTER);
            } else if (fieldType.equals(Date.class)) {
                return Date.valueOf(LocalDate.parse(value, DATE_FORMATTER)); 
            }
        } catch (Exception e) {
            System.err.println("Erreur de parsing pour la valeur : " + value + " et le type : " + fieldType.getSimpleName());
        }
        return null;
    }
}
