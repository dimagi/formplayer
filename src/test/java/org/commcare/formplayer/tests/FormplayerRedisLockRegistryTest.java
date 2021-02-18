package org.commcare.formplayer.tests;

import org.commcare.formplayer.services.FormplayerRedisLockRegistry;
import org.junit.jupiter.api.Test;

public class FormplayerRedisLockRegistryTest {

    @Test
    public void testStuff() {
        FormplayerRedisLockRegistry registry = new FormplayerRedisLockRegistry();
        registry.obtain("");
        int x = 3;
    }

}
