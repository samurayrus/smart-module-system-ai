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
        sql = sql.trim().toLowerCase();

        try {
            if (sql.startsWith("select")) {
                // Для запросов с возвратом данных
                return jdbcTemplate.queryForList(sql);
            } else if (sql.startsWith("insert") || sql.startsWith("update")
                    || sql.startsWith("delete") || sql.startsWith("merge")) {
                // Для изменяющих запросов
                return jdbcTemplate.update(sql);
            } else if (sql.startsWith("create") || sql.startsWith("alter")
                    || sql.startsWith("drop") || sql.startsWith("truncate")) {
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
