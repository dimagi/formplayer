package org.commcare.formplayer.objects;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Created by willpride on 8/1/16.
 */

@Entity
@Table(name = "menu_sessions")
@Getter
public class SerializableMenuSession {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(updatable = false)
    private String username;

    @Column(updatable = false)
    private String domain;

    @Column(name = "appid", updatable = false)
    private String appId;

    @Column(name = "installreference", updatable = false)
    private String installReference;

    @Column(name = "locale", updatable = false)
    private String locale;

    @Column(name = "asuser", updatable = false)
    private String asUser;

    @Column(updatable = false)
    private boolean preview;

    @Setter
    @Column(name = "commcaresession")
    private byte[] commcareSession;

    public SerializableMenuSession() {
    }

    public SerializableMenuSession(String username, String domain, String appId,
                                   String installReference, String locale,
                                   String asUser, boolean preview) {
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.locale = locale;
        this.asUser = asUser;
        this.preview = preview;
    }
}
