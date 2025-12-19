package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment implements Serializable {
    private static final long serialVersionUID = -7185076331018398831L;
    private final String id;
    private final Date lastModifiedDate;
    private final String name;
    private final String contentType;
    private final String contentBytes;
    private final boolean isInline;

    public Attachment(final String id,
                      final Date lastModifiedDate,
                      final String name,
                      final String contentType,
                      final String contentBytes,
                      final boolean isInline) {
        this.id = id;
        this.lastModifiedDate = new Date(lastModifiedDate.getTime());
        this.name = name;
        this.contentType = contentType;
        this.contentBytes = contentBytes;
        this.isInline = isInline;
    }

    public boolean isSignatureAttachment() {
        return isInline && contentType.toLowerCase().startsWith("image");
    }

    public boolean isNotSignatureAttachment() {
        return !isSignatureAttachment();
    }

    public String getId() {
        return id;
    }

    public Date getLastModifiedDate() {
        return new Date(lastModifiedDate.getTime());
    }

    public String getName() {
        return this.name;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getContentBytes() {
        return this.contentBytes;
    }
}
