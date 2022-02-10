package org.commcare.formplayer.objects;

import java.time.Instant;
import java.util.Map;

/**
 * Projection of SerializableFormSession with only fields
 * required by the ``get_session`` view
 */
public interface FormSessionListView {
    String getId();

    String getTitle();

    Instant getDateCreated();

    Map<String, String> getSessionData();
}
