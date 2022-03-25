package org.commcare.formplayer.objects;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name="form_definition")
@EntityListeners(AuditingEntityListener.class)
@Getter
public class SerializableFormDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name="datecreated")
    private Instant dateCreated;

    @Column(name="appid")
    private String appId;

    @Column(name="formxmlns")
    private String formXmlns;

    @Column(name="formversion")
    private String formVersion;

    @Column(name="formdef")
    private String serializedFormDef;

    protected SerializableFormDefinition(){}
    public SerializableFormDefinition(String appId, String formXmlns, String formVersion, String formDef) {
        this.appId = appId;
        this.formXmlns = formXmlns;
        this.formVersion = formVersion;
        this.serializedFormDef = formDef;
    }
}
