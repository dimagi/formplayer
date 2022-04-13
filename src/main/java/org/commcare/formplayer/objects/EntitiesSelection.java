package org.commcare.formplayer.objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "entities_selection")
public class EntitiesSelection {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(updatable = false)
    private String username;

    @Column(updatable = false)
    private String domain;

    @Column(name = "appid", updatable = false)
    private String appId;

    @Column(name = "asuser", updatable = false)
    private String asUser;

    @Column(updatable = false)
    @Convert(converter = ByteArrayConverter.class)
    private String[] entities;

    @CreationTimestamp
    @Column(name = "datecreated")
    private Instant dateCreated;

    @SuppressWarnings("unused")
    public EntitiesSelection() {
    }

    public EntitiesSelection(String username, String domain, String appId, String asUser, String[] entities) {
        this.entities = entities;
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.asUser = asUser;
    }

    public String[] getEntities() {
        return entities;
    }

    public UUID getId() {
        return id;
    }

    public Instant getDateCreated() {
        return dateCreated;
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
}
