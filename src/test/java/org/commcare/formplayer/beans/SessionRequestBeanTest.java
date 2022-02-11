package org.commcare.formplayer.beans;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionRequestBeanTest {
    @ParameterizedTest
    @ValueSource(strings = {"session-id", "session_id", "sessionId"})
    public void testFromJson(String var) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = String.format("{\"%s\": \"1\"}", var);
        SessionRequestBean bean = objectMapper.readValue(jsonData, SessionRequestBean.class);
        assertThat(bean.getSessionId()).isEqualTo("1");
    }
}
