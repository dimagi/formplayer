package org.commcare.formplayer.objects;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * Created by willpride on 8/1/16.
 */

@Entity
@Table(name="menu_sessions")
@Getter
public class SerializableMenuSession {

    @Id @GeneratedValue(generator="uuid")
    @GenericGenerator(name="uuid", strategy="org.hibernate.id.UUIDGenerator")
    private String id;

    private String username;

    private String domain;

    @Column(name="appid")
    private String appId;

    @Column(name="installreference")
    private String installReference;

    @Column(name="locale")
    private String locale;

    @Setter
    @Column(name="commcaresession")
    private byte[] commcareSession;

    @Column(name="asuser")
    private String asUser;

    @Transient
    private boolean oneQuestionPerScreen;

    private boolean preview;

    public SerializableMenuSession(){}

    public SerializableMenuSession(String username, String domain, String appId,
                                   String installReference, String locale, byte[] commcareSession,
                                   boolean oneQuestionPerScreen,
                                   String asUser, boolean preview){
        this.username = username;
        this.domain = domain;
        this.appId = appId;
        this.installReference = installReference;
        this.locale = locale;
        this.commcareSession = commcareSession;
        this.asUser = asUser;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.preview = preview;
    }
}
