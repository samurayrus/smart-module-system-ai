package ru.samurayrus.smartmodulesystemai.workers.database;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.samurayrus.smartmodulesystemai.gui.ContextStorage;
import ru.samurayrus.smartmodulesystemai.workers.WorkerEventDataBus;
import ru.samurayrus.smartmodulesystemai.workers.WorkerListener;
import ru.samurayrus.smartmodulesystemai.workers.fileeditor.Command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Воркер отвечает за работу с базой данных.
 * Свобода выполнения SQL запросов с гибким распределением по типам операций (SELECT, INSERT, CREATE, ADD, etc)
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.modules.databaseworker", name = "enabled", havingValue = "true")
public class WorkerDatabaseService implements WorkerListener {
    private final LlmSqlResponseParser responseParser = new LlmSqlResponseParser();
    private final JdbcTemplate jdbcTemplate;
    private final ContextStorage contextStorage;
    private final WorkerEventDataBus workerEventDataBus;

    @Autowired
    public WorkerDatabaseService(@Qualifier("jdbc-template-master") JdbcTemplate jdbcTemplate,
                                 ContextStorage contextStorage,
                                 WorkerEventDataBus workerEventDataBus) {
        this.jdbcTemplate = jdbcTemplate;
        this.contextStorage = contextStorage;
        this.workerEventDataBus = workerEventDataBus;
    }

    @PostConstruct
    void registerWorker() {
        log.info("DataBaseWorker init registration as worker...");
        workerEventDataBus.registerWorker(this);

        Map<String, String> tagsMag = new HashMap<>();
        tagsMag.put("<SQL_START>", "<span style='color:green;'>&lt;SQL_START&gt;");
        tagsMag.put("<SQL_END>", "&lt;SQL_END&gt;</span>");
        contextStorage.addReplacerSpecialTagFromWorkerToGuiMessages(tagsMag);
    }

    @Override
    public boolean callWorker(Command command) {
        return false;
    }

    @Override
    public boolean callWorker(String content, boolean toolMode) {
        LlmSqlParsedResponse llmSqlParsedResponse = responseParser.parseResponse(content);

        if (llmSqlParsedResponse.isHasSql()) {
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", "[Запрос к бд]: " + llmSqlParsedResponse.getSqlQuery());
            String result;
            try {
                result = "[Ответ успешен! Sql запрос выполнен]: " + processSqlResult(executeSqlAndReturnAllAnswers(llmSqlParsedResponse.getSqlQuery()));
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                result = "[Ошибка при выполнении запроса] StackTrace: \n " + sw;
            }
            contextStorage.addMessageToContextAndMessagesListIfEnabled("tool", result);
            return true;
        }
        return false;
    }

    private Object executeSqlAndReturnAllAnswers(String sql) {
        log.info("Database Worker:  Запрос к бд {}", sql);
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
