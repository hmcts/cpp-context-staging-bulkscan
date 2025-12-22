package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDetails implements Serializable {
    private static final long serialVersionUID = -9135137105553059979L;

    private final String metadata_field_name;

    private final String metadata_field_value;

    public MetadataDetails(final String metadata_field_name, final String metadata_field_value) {
        this.metadata_field_name = metadata_field_name;
        this.metadata_field_value = metadata_field_value;
    }

    public String getMetadata_field_name() {
        return metadata_field_name;
    }

    public String getMetadata_field_value() {
        return metadata_field_value;
    }

    public static Builder metadataDetails() {
        return new MetadataDetails.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj == null || getClass() != obj.getClass()) { return false; }
        final MetadataDetails that = (MetadataDetails) obj;

        return java.util.Objects.equals(this.metadata_field_name, that.metadata_field_name) &&
                java.util.Objects.equals(this.metadata_field_value, that.metadata_field_value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(metadata_field_name, metadata_field_value);
    }

    @Override
    public String toString() {
        return "MetadataDetails{" +
                "metadata_field_name='" + metadata_field_name + "'," +
                "metadata_field_value='" + metadata_field_value + "'" +
                "}";
    }

    public static class Builder {
        private String metadata_field_name;

        private String metadata_field_value;

        public Builder withMetadata_field_name(final String metadata_field_name) {
            this.metadata_field_name = metadata_field_name;
            return this;
        }

        public Builder withMetadata_field_value(final String metadata_field_value) {
            this.metadata_field_value = metadata_field_value;
            return this;
        }

        public MetadataDetails build() {
            return new MetadataDetails(metadata_field_name, metadata_field_value);
        }
    }
}
