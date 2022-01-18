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

    @Column(name="appid", updatable = false)
    private String appId;

    @Column(name="appversion", updatable = false)
    private String appVersion;

    @Column(name="xmlns", updatable = false)
    private String xmlns;

    @Column(name="formdef", updatable = false)
    private String serializedFormDef;

    protected SerializableFormDefinition(){}
    public SerializableFormDefinition(String appId, String appVersion, String formXmlns, String formdef) {
        this.appId = appId;
        this.appVersion = appVersion;
        this.xmlns = formXmlns;
        this.serializedFormDef = formdef;
    }
}
