package org.commcare.formplayer.objects;

import org.commcare.formplayer.util.Constants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.javarosa.core.model.instance.TreeElement;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Model class representing a postgress entry for virtual data instances table
 */
@Entity
@Table(name = Constants.POSTGRES_VIRTUAL_DATA_INSTANCE_TABLE_NAME)
public class SerializableDataInstance {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

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
            String domain, String appId, String asUser, TreeElement instanceXml, boolean useCaseTemplate) {
        this.instanceId = instanceId;
        this.reference = reference;
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.asUser = asUser;
        this.instanceXml = instanceXml;
        this.useCaseTemplate = useCaseTemplate;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public TreeElement getInstanceXml() {
        return instanceXml;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getAppId() {
        return appId;
    }

    public String getAsUser() {
        return asUser;
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public String getReference() {
        return reference;
    }

    public boolean useCaseTemplate() {
        return useCaseTemplate;
    }
}
