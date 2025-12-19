package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailDetails implements Serializable {
    private static final long serialVersionUID = 9205841299835327173L;
    private final String subject;
    private final String from;
    private final String body;
    private final List<Attachment> attachments;

    public EmailDetails(final String subject, final String from, final String body, final List<Attachment> attachments) {
        this.subject = subject;
        this.from = from;
        this.body = body;
        this.attachments = new ArrayList<>(attachments);
    }

    public static EmailDetails.EmailDetailsBuilder builder() {
        return new EmailDetails.EmailDetailsBuilder();
    }

    public String getSubject() {
        return this.subject;
    }

    public String getFrom() {
        return this.from;
    }

    public String getBody() {
        return this.body;
    }

    public List<Attachment> getAttachments() {
        return new ArrayList<>(this.attachments);
    }

    public static class EmailDetailsBuilder {
        private String subject;
        private String from;
        private String body;
        private List<Attachment> attachments = new ArrayList<>();

        EmailDetailsBuilder() {
        }

        public EmailDetails.EmailDetailsBuilder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        public EmailDetails.EmailDetailsBuilder from(final String from) {
            this.from = from;
            return this;
        }

        public EmailDetails.EmailDetailsBuilder body(final String body) {
            this.body = body;
            return this;
        }

        public EmailDetails.EmailDetailsBuilder attachments(final List<Attachment> attachments) {
            this.attachments = new ArrayList<>(attachments);
            return this;
        }

        public EmailDetails build() {
            return new EmailDetails(this.subject, this.from, this.body, this.attachments);
        }

    }
}