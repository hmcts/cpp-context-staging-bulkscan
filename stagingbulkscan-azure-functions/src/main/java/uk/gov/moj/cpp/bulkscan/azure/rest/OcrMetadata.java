package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrMetadata implements Serializable {
    private static final long serialVersionUID = 2882928460458050629L;

    private final List<MetadataDetails> Metadata_file;

    public OcrMetadata(final List<MetadataDetails> Metadata_file) {
        this.Metadata_file = Metadata_file;
    }

    public List<MetadataDetails> getMetadata_file() {
        return Metadata_file;
    }

    public static Builder ocrMetadata() {
        return new OcrMetadata.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final OcrMetadata that = (OcrMetadata) obj;

        return java.util.Objects.equals(this.Metadata_file, that.Metadata_file);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(Metadata_file);
    }

    @Override
    public String toString() {
        return "OcrMetadata{" +
                "Metadata_file='" + Metadata_file + "'" +
                "}";
    }

    public static class Builder {
        private List<MetadataDetails> Metadata_file;

        public Builder withMetadata_file(final List<MetadataDetails> Metadata_file) {
            this.Metadata_file = Metadata_file;
            return this;
        }

        public OcrMetadata build() {
            return new OcrMetadata(Metadata_file);
        }
    }
}
