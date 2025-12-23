package uk.gov.moj.cpp.bulkscan.azure.function.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncryptedAttachment implements Serializable {
    private static final long serialVersionUID = -7185076331018398831L;
    private final String url;
    private final String encryption_key;
    private final String encryption_iv;
    private final String mimetype;
    private final String filename;

    public EncryptedAttachment(final String url,
                               final String encryption_key,
                               final String encryption_iv,
                               final String mimetype,
                               final String filename) {
        this.url = url;
        this.encryption_key = encryption_key;
        this.encryption_iv = encryption_iv;
        this.mimetype = mimetype;
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }

    public String getEncryption_key() {
        return encryption_key;
    }

    public String getEncryption_iv() {
        return encryption_iv;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getFilename() {
        return filename;
    }
}
