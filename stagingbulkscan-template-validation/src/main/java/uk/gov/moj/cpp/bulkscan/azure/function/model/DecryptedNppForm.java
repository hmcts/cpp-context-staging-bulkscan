package uk.gov.moj.cpp.bulkscan.azure.function.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecryptedNppForm implements Serializable {
    private static final long serialVersionUID = 9205841299835327173L;
    private final String serviceSlug;
    private final String submissionId;
    private final SubmissionAnswers submissionAnswers;
    private final List<EncryptedAttachment> attachments;

    public DecryptedNppForm(final String serviceSlug, final String submissionId, final SubmissionAnswers submissionAnswers, final List<EncryptedAttachment> attachments) {
        this.serviceSlug = serviceSlug;
        this.submissionId = submissionId;
        this.submissionAnswers = submissionAnswers;
        this.attachments = new ArrayList<>(attachments);
    }

    public static DecryptedNppFormBuilder builder() {
        return new DecryptedNppFormBuilder();
    }

    public String getServiceSlug() {
        return serviceSlug;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public SubmissionAnswers getSubmissionAnswers() {
        return submissionAnswers;
    }

    public List<EncryptedAttachment> getAttachments() {
        return attachments;
    }

    public static class DecryptedNppFormBuilder {
        private String serviceSlug;
        private String submissionId;
        private SubmissionAnswers submissionAnswers;
        private List<EncryptedAttachment> attachments = new ArrayList<>();

        DecryptedNppFormBuilder() {
        }

        public DecryptedNppFormBuilder serviceSlug(final String serviceSlug) {
            this.serviceSlug = serviceSlug;
            return this;
        }

        public DecryptedNppFormBuilder submissionId(final String submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public DecryptedNppFormBuilder submissionAnswers(final SubmissionAnswers submissionAnswers) {
            this.submissionAnswers = submissionAnswers;
            return this;
        }

        public DecryptedNppFormBuilder attachments(final List<EncryptedAttachment> attachments) {
            this.attachments = new ArrayList<>(attachments);
            return this;
        }

        public DecryptedNppForm build() {
            return new DecryptedNppForm(this.serviceSlug, this.submissionId, this.submissionAnswers, this.attachments);
        }
    }
}