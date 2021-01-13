package org.commcare.formplayer.db.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class LockLoggingCallback implements Callback {

    private final Log log = LogFactory.getLog(LockLoggingCallback.class);

    @Override
    public boolean supports(Event event, Context context) {
        return event.equals(Event.BEFORE_MIGRATE);
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    private void log(Event event, String message, Object ...params) {
        List<Object> allParams = new ArrayList<Object>(Arrays.asList(params));
        allParams.add(0, event);
        log.info(String.format("[%s] " + message, allParams.toArray()));
    }

    @Override
    public void handle(Event event, Context context) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        List<Map<String, Object>> locks = jdbcTemplate.queryForList(String.format(
                "SELECT * FROM pg_locks WHERE locktype = 'advisory'"
        ));
        if (locks.isEmpty()) {
            this.log(event, "No advisory locks in DB");
            return;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        for (Map<String, Object> row : locks){
            try {
                this.log(event, objectMapper.writeValueAsString(row));
            } catch (JsonProcessingException e) {
                log.error("Error converting row to JSON", e);
            }
        }
    }
}