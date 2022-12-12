package org.commcare.formplayer.objects;

import lombok.Getter;
import lombok.Setter;

import org.commcare.formplayer.util.Constants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.Instant;

/**
 * Model class representing a postgres entry for media metadata table
 */
@Entity
@Table(name = Constants.POSTGRES_MEDIA_META_DATA_TABLE_NAME)
@Getter
public class MediaMetadataRecord {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    @Column(name = "filePath", updatable = false)
    private String filePath;

    @Setter
    @Column(name = "formSessionId")
    private String formSessionId;

    @Column(name = "contentType", updatable = false)
    private String contentType;

    @Column(name = "contentLength", updatable = false)
    private Integer contentLength;

    @Column(name = "username", updatable = false)
    private String username;

    @Column(name = "asUser", updatable = false)
    private String asUser;

    @Column(updatable = false)
    private String domain;

    @Column(name = "appid", updatable = false)
    private String appid;

    @CreationTimestamp
    @Column(name = "datecreated")
    private Instant datecreated;

    public MediaMetadataRecord() { }

    public MediaMetadataRecord(
            String filePath,
            String contentType,
            Integer contentLength,
            String username,
            String asUser,
            String domain,
            String appid) {
        this.filePath = filePath;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.username = username;
        this.asUser = asUser;
        this.domain = domain;
        this.appid = appid;
    }
}
