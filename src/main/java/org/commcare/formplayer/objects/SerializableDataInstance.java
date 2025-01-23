package org.commcare.formplayer.objects;

import org.commcare.core.process.CommCareInstanceInitializer;
import org.commcare.data.xml.VirtualInstances;
import org.commcare.formplayer.util.Constants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.ExternalDataInstanceSource;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.utils.TreeUtilities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Model class representing a postgress entry for virtual data instances table
 */
@Entity
@Table(name = Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME)
@Getter
public class SerializableDataInstance {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Setter
    @Column(name = "key", updatable = false)
    private String namespacedKey;

    @Column(name = "instanceid", updatable = false)
    private String instanceId;

    @Column(name = "reference", updatable = false)
    private String reference;

    @Column(name = "usecasetemplate", updatable = false)
    private boolean useCaseTemplate;

    @Column(updatable = false)
    private String username;

    @Column(updatable = false)
    private String domain;

    @Column(name = "appid", updatable = false)
    private String appId;

    @Column(name = "asuser", updatable = false)
    private String asUser;

    @Column(name = "instancexml", updatable = false)
    @Convert(converter = TreeElementConverter.class)
    private TreeElement instanceXml;

    @CreationTimestamp
    @Column(name = "datecreated")
    private Instant dateCreated;

    @SuppressWarnings("unused")
    public SerializableDataInstance() {
    }

    public SerializableDataInstance(String instanceId, String reference, String username,
            String domain, String appId, String asUser, TreeElement instanceXml, boolean useCaseTemplate,
            String namespacedKey) {
        this.instanceId = instanceId;
        this.reference = reference;
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.asUser = asUser;
        this.instanceXml = instanceXml;
        this.useCaseTemplate = useCaseTemplate;
        this.namespacedKey = namespacedKey;
    }

    /**
     * @param instanceId The instance ID that is being requested.
     * @param key The storage key without the namespace
     * @return
     */
    public ExternalDataInstance toInstance(String instanceId, String key, String refId) {
        TreeElement root = getInstanceXml();
        if (!instanceId.equals(getInstanceId())) {
            root = TreeUtilities.renameInstance(root, instanceId);
        }
        String reference = getReference();
        String newReference;
        if (CommCareInstanceInitializer.isNonUniqueReference(reference)) {
            // If old reference shceme we don't wanna change anything
            // should be removed once we migrate all instances to new unique reference scheme
            newReference = reference;
        } else {
            String refScheme = VirtualInstances.getReferenceScheme(reference);
            newReference = VirtualInstances.getInstanceReference(refScheme, refId);
        }
        ExternalDataInstanceSource instanceSource = ExternalDataInstanceSource.buildVirtual(
                        instanceId, root, newReference, isUseCaseTemplate(), key);
        return instanceSource.toInstance();
    }
}
