package org.commcare.formplayer.objects;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Java representation of form_definition sql table
 * Used to share form definitions across sessions
 */
@Entity
@Table(name = "form_definition")
@EntityListeners(AuditingEntityListener.class)
@Getter
public class SerializableFormDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "datecreated")
    private Instant dateCreated;

    @Column(name = "appid")
    private String appId;

    @Column(name = "formxmlns")
    private String formXmlns;

    @Column(name = "formversion")
    private String formVersion;

    @Column(name = "formdef")
    private String serializedFormDef;

    public void setSerializedFormDef(String serializedFormDef) {
        this.serializedFormDef = serializedFormDef;
    }

    protected SerializableFormDefinition() {}

    public SerializableFormDefinition(String appId, String formXmlns, String formVersion, String formDef) {
        this.appId = appId;
        this.formXmlns = formXmlns;
        this.formVersion = formVersion;
        this.serializedFormDef = formDef;
    }
}
