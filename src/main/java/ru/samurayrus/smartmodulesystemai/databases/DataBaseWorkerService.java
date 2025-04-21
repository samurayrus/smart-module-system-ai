package ru.samurayrus.smartmodulesystemai.databases;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DataBaseWorkerService {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataBaseWorkerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Object executeSql(String sql) {
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
}
