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

    @Column(name="appversion")
    private String appVersion;

    @Column(name="xmlns")
    private String xmlns;

    @Column(name="formdef")
    private String serializedFormDef;

    protected SerializableFormDefinition(){}
    public SerializableFormDefinition(String appId, String appVersion, String formXmlns, String formdef) {
        this.appId = appId;
        this.appVersion = appVersion;
        this.xmlns = formXmlns;
        this.serializedFormDef = formdef;
    }
}
