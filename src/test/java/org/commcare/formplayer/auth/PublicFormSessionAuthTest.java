package org.commcare.formplayer.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

public class PublicFormSessionAuthTest {

    @Test
    public void getAuthHeaders_emitsPublicCookieAndHeaderOnly() {
        HttpHeaders headers = new PublicFormSessionAuth("session-key-123").getAuthHeaders();

        assertEquals("public_form_session_key=session-key-123", headers.getFirst("Cookie"));
        assertEquals("true", headers.getFirst("CommCare-Public-Session"));

        // Exactly the two public headers — no Django sessionid/Authorization leaks out.
        assertEquals(2, headers.size());
        assertFalse(headers.containsKey("sessionid"));
        assertFalse(headers.containsKey("Authorization"));
    }

    @Test
    public void toString_doesNotLeakTheKey() {
        assertFalse(new PublicFormSessionAuth("super-secret-key").toString().contains("super-secret-key"));
    }

    @Test
    public void constructor_rejectsMissingKey() {
        assertThrows(IllegalArgumentException.class, () -> new PublicFormSessionAuth(null));
        assertThrows(IllegalArgumentException.class, () -> new PublicFormSessionAuth(""));
    }
}
