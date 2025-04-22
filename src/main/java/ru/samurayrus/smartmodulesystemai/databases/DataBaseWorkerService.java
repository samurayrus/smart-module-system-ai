package ru.samurayrus.smartmodulesystemai.databases;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataBaseWorkerService {
    private final JdbcTemplate jdbcTemplate;
    private final LlmResponseParser responseParser;

    @Autowired
    public DataBaseWorkerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.responseParser = new LlmResponseParser();
    }

    public String executeSql(String sql) {
        return processSqlResult(executeSqlAndReturnAllAnswers(sql));
    }

    private Object executeSqlAndReturnAllAnswers(String sql) {
        System.out.println("\n ---- \n Запрос к бд {" + sql + "} \n ---");
        sql = sql.trim();
        String sqlForFilter = sql.toLowerCase();

        try {
            if (sqlForFilter.startsWith("select")) {
                // Для запросов с возвратом данных
                return jdbcTemplate.queryForList(sql);
            } else if (sqlForFilter.startsWith("insert") || sqlForFilter.startsWith("update")
                    || sqlForFilter.startsWith("delete") || sqlForFilter.startsWith("merge")) {
                // Для изменяющих запросов
                return jdbcTemplate.update(sql);
            } else if (sqlForFilter.startsWith("create") || sqlForFilter.startsWith("alter")
                    || sqlForFilter.startsWith("drop") || sqlForFilter.startsWith("truncate")) {
                // Для DDL-запросов
                jdbcTemplate.execute(sql);
                return "DDL операция выполнена успешно";
            } else {
                // Для других случаев (хранимые процедуры и т.д.)
                return jdbcTemplate.queryForList(sql);
            }
        } catch (DataAccessException e) {

            throw new RuntimeException("Ошибка выполнения SQL: " + e.getMessage(), e);
        }
    }


    public String processSqlResult(Object sqlResult) {
        if (sqlResult == null) {
            return "Нет данных";
        }

        if (sqlResult instanceof List) {
            return formatListResult((List<?>) sqlResult);
        } else if (sqlResult instanceof Map) {
            return formatMapResult((Map<?, ?>) sqlResult);
        } else if (sqlResult instanceof Integer || sqlResult instanceof Long) {
            return "Операция успешна! Теперь можно отвечать пользователю или выполнять другие команды, которые необходимы для выполнения текущей задачи. Затронуто строк: " + sqlResult;
        } else {
            return sqlResult.toString();
        }
    }


    private String formatListResult(List<?> list) {
        if (list.isEmpty()) {
            return "Нет данных";
        }

        StringBuilder sb = new StringBuilder();
        if (list.get(0) instanceof Map) {
            // Результат с несколькими колонками
            for (Object item : list) {
                Map<?, ?> row = (Map<?, ?>) item;
                sb.append(row.values().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" | ")))
                        .append("\n");
            }
        } else {
            // Результат с одной колонкой
            list.forEach(item -> sb.append(item.toString()).append("\n"));
        }
        return sb.toString();
    }

    private String formatMapResult(Map<?, ?> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
    }
}
