package uk.gov.moj.cpp.bulkscan.azure.function.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncryptedNppForm implements Serializable {
    private static final long serialVersionUID = 9205841299835327173L;
    private final String token;

    public EncryptedNppForm(final String token) {
        this.token = token;
    }

    public static EncryptedNppFormBuilder builder() {
        return new EncryptedNppFormBuilder();
    }

    public String getToken() {
        return this.token;
    }

    public static class EncryptedNppFormBuilder {
        private String token;

        EncryptedNppFormBuilder() {
        }

        public EncryptedNppFormBuilder token(final String token) {
            this.token = token;
            return this;
        }

        public EncryptedNppForm build() {
            return new EncryptedNppForm(this.token);
        }
    }
}