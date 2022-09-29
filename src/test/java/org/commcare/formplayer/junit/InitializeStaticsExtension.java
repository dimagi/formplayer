package org.commcare.formplayer.junit;

import org.commcare.formplayer.application.SQLiteProperties;
import org.commcare.formplayer.engine.ClasspathFileRoot;
import org.commcare.formplayer.util.PrototypeUtils;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.LocalizerManager;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Perform initialization of static utils.
 */
public class InitializeStaticsExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        LocalizerManager.setUseThreadLocalStrategy(true);
        Localization.getGlobalLocalizerAdvanced().addAvailableLocale("default");
        Localization.setLocale("default");
        ReferenceManager.instance().addReferenceFactory(new ClasspathFileRoot());
        Localization.registerLanguageReference("default",
                "jr://springfile/formplayer_translatable_strings.txt");

        PrototypeUtils.setupThreadLocalPrototypes();

        DateUtils.setTimezoneProvider(new MockTimezoneProvider());
    }
}
