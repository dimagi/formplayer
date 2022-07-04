package org.commcare.formplayer.services;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.commcare.core.interfaces.VirtualDataInstanceStorage;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Used to get external instances either by making a HTTP call or fetching them from DB
 */
public class FormplayerRemoteInstanceFetcher implements RemoteInstanceFetcher {

    private final CaseSearchHelper caseSearchHelper;
    private final VirtualDataInstanceStorage mVirtualDataInstanceStorage;

    public FormplayerRemoteInstanceFetcher(CaseSearchHelper caseSearchHelper,
            VirtualDataInstanceStorage virtualDataInstanceStorage) {
        this.caseSearchHelper = caseSearchHelper;
        this.mVirtualDataInstanceStorage = virtualDataInstanceStorage;
    }

    @Override
    public TreeElement getExternalRoot(String instanceId, ExternalDataInstanceSource source)
            throws RemoteInstanceException {
        if (source.getSourceUri() != null) {
            try {
                return caseSearchHelper.getExternalRoot(instanceId, (source), false);
            } catch (XmlPullParserException | UnfullfilledRequirementsException | InvalidStructureException e) {
                throw new RemoteInstanceException("Invalid data retrieved from remote instance " +
                        instanceId + ". If the error persists please contact your help desk.", e);
            } catch (IOException e) {
                throw new RemoteInstanceException("Could not retrieve data for remote instance "
                        + instanceId + ". Please try opening the form again.", e);
            }
        } else if (source.getStorageReferenceId() != null) {
            ExternalDataInstance instance = mVirtualDataInstanceStorage.read(source.getStorageReferenceId(), instanceId);
            return (TreeElement)instance.getRoot();
        }
        throw new RemoteInstanceException("Could not retrieve data for instance " + instanceId
                + ". Implementations for ExternalDataInstanceSource must define one of sourceUri or "
                + "storageRefernceId");
    }

    public VirtualDataInstanceStorage getVirtualDataInstanceStorage() {
        return mVirtualDataInstanceStorage;
    }
}
