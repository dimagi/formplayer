package org.commcare.formplayer.junit;

import org.javarosa.core.model.utils.TimezoneProvider;

public class MockTimezoneProvider extends TimezoneProvider {

    private int offsetMillis;

    public void setOffset(int offset) {
        this.offsetMillis = offset;
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return offsetMillis;
    }
}
