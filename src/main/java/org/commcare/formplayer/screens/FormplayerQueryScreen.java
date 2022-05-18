package org.commcare.formplayer.screens;

import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.commcare.util.screen.QueryScreen;

/**
 * Created by willpride on 8/7/16.
 */
public class FormplayerQueryScreen extends QueryScreen {

    public FormplayerQueryScreen(VirtualDataInstanceStorage instanceStorage) {
        super(null, null, null, instanceStorage);
    }

}
