package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentMetadata {
    private String fileName;
    private String urn;
    private String oucode;
    private Attachment attachment;

    AttachmentMetadata(final String fileName, final String urn, final String oucode, final Attachment attachment) {
        this.fileName = fileName;
        this.urn = urn;
        this.oucode = oucode;
        this.attachment = attachment;
    }

    public static AttachmentMetadata.AttachmentMetadataBuilder builder() {
        return new AttachmentMetadata.AttachmentMetadataBuilder();
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getUrn() {
        return this.urn;
    }

    public String getOucode() {
        return this.oucode;
    }

    public Attachment getAttachment() {
        return this.attachment;
    }

    public void setAttachment(final Attachment attachment) {
        this.attachment = attachment;
    }


    public String toString() {
        return "AttachmentMetadata(fileName=" + this.getFileName() + ", urn=" + this.getUrn() + ", oucode=" + this.getOucode() + ", attachment=" + this.getAttachment() + ")";
    }

    public static class AttachmentMetadataBuilder {
        private String fileName;
        private String urn;
        private String oucode;
        private Attachment attachment;

        AttachmentMetadataBuilder() {
        }

        public AttachmentMetadata.AttachmentMetadataBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public AttachmentMetadata.AttachmentMetadataBuilder urn(final String urn) {
            this.urn = urn;
            return this;
        }

        public AttachmentMetadata.AttachmentMetadataBuilder oucode(final String oucode) {
            this.oucode = oucode;
            return this;
        }

        public AttachmentMetadata.AttachmentMetadataBuilder attachment(final Attachment attachment) {
            this.attachment = attachment;
            return this;
        }

        public AttachmentMetadata build() {
            return new AttachmentMetadata(this.fileName, this.urn, this.oucode, this.attachment);
        }

        public String toString() {
            return "AttachmentMetadata.AttachmentMetadataBuilder(fileName=" + this.fileName + ", urn=" + this.urn + ", oucode=" + this.oucode + ", attachment=" + this.attachment + ")";
        }
    }
}