package org.commcare.formplayer.installers;

import lombok.NonNull;

import org.commcare.resources.ResourceInstallContext;
import org.commcare.resources.model.*;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.LocalizationUtils;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * ResourceInstaller for locale files that always persists the locale data in
 * the SQLite DB.
 */
public class FormplayerLocaleInstaller extends SimpleInstaller {

    private String locale;

    private Hashtable<String, String> localizedValues;

    /**
     * Serialization only!
     */
    @SuppressWarnings("unused")
    public FormplayerLocaleInstaller() {
    }

    public FormplayerLocaleInstaller(@NonNull String locale) {
        this.locale = locale;
    }

    @Override
    public boolean initialize(CommCarePlatform platform, boolean isUpgrade) throws IOException, InvalidReferenceException, InvalidStructureException, XmlPullParserException, UnfullfilledRequirementsException {
        Localization.getGlobalLocalizerAdvanced().addAvailableLocale(locale);
        Localization.getGlobalLocalizerAdvanced().registerLocaleResource(locale, new TableLocaleSource(localizedValues));
        return true;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, CommCarePlatform platform, boolean upgrade, ResourceInstallContext resourceInstallContext) throws UnresolvedResourceException {
        try {
            try (InputStream incoming = ref.getStream()) {
                localizedValues = LocalizationUtils.parseLocaleInput(incoming);
                table.commit(r, upgrade ? Resource.RESOURCE_STATUS_UPGRADE : Resource.RESOURCE_STATUS_INSTALLED);
            }
            return true;
        } catch (IOException e) {
            throw new UnreliableSourceException(r, e.getMessage());
        }
    }

    @Override
    public boolean verifyInstallation(Resource r, Vector<MissingMediaException> problemList, CommCarePlatform platform) {
        return true;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        locale = ExtUtil.readString(in);
        localizedValues = (Hashtable)ExtUtil.nullIfEmpty(
                (Hashtable)ExtUtil.read(in, new ExtWrapMap(String.class, String.class), pf)
        );
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, locale);
        ExtUtil.write(out, new ExtWrapMap(ExtUtil.emptyIfNull(localizedValues)));
    }
}
