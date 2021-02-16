package org.commcare.formplayer.objects;

import java.time.Instant;
import java.util.Map;

/**
 * Projection of SerializableFormSession with only fields
 * required by the ``get_session`` view
 *
 * This is a temporary projection that can be removed once the
 * ``dateOpened`` field is no longer required.
 */
public interface FormSessionListDetailsViewRaw {
    String getId();
    String getTitle();
    String getDateOpened();
    Instant getDateCreated();
    byte[] getSessionData();
}
