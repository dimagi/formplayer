package org.commcare.formplayer.objects;

import org.commcare.formplayer.util.Constants;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Model class representing a postgres entry for media metadata table
 */
@Entity
@Table(name = Constants.POSTGRES_MEDIA_META_DATA_TABLE_NAME)
@Getter
public class MediaMetadataRecord {

    @Id
    private String id;

    @Column(name = "filepath", updatable = false)
    private String filePath;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formsessionid")
    private SerializableFormSession formSession;

    @Column(name = "contenttype", updatable = false)
    private String contentType;

    @Column(name = "contentlength", updatable = false)
    private Integer contentLength;

    @Column(name = "username", updatable = false)
    private String username;

    @Column(name = "asuser", updatable = false)
    private String asUser;

    @Column(updatable = false)
    private String domain;

    @Column(name = "appid", updatable = false)
    private String appId;

    @CreationTimestamp
    @Column(name = "datecreated")
    private Instant datecreated;

    public MediaMetadataRecord() {
    }

    public MediaMetadataRecord(
            String id,
            String filePath,
            SerializableFormSession formSession,
            String contentType,
            Integer contentLength,
            String username,
            String asUser,
            String domain,
            String appId) {
        this.id = id;
        this.filePath = filePath;
        this.formSession = formSession;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.username = username;
        this.asUser = asUser;
        this.domain = domain;
        this.appId = appId;
    }

    @Override
    public String toString() {
        String formSessionId = "";
        if (formSession != null) {
            formSessionId = formSession.getId();
        }
        return "MediaMetaData [id=" + id + ", formSessionId=" + formSessionId + ", username=" + username
                + ", asUser=" + asUser + " domain=" + domain + ", filePath=" + filePath
                + ", contentType=" + contentType + "]";
    }

    public String getId() {
        return id;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContentType() {
        return contentType;
    }
}
