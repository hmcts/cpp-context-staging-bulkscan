package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Offence implements Serializable {
    private static final long serialVersionUID = -2287352870561308209L;

    private final PleaValue pleaValue;

    private final String title;

    public Offence(final PleaValue pleaValue, final String title) {
        this.pleaValue = pleaValue;
        this.title = title;
    }

    public PleaValue getPleaValue() {
        return pleaValue;
    }

    public String getTitle() {
        return title;
    }

    public static Builder offence() {
        return new Offence.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Offence that = (Offence) obj;

        return java.util.Objects.equals(this.pleaValue, that.pleaValue) &&
                java.util.Objects.equals(this.title, that.title);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pleaValue, title);
    }

    @Override
    public String toString() {
        return "Offence{" +
                "pleaValue='" + pleaValue + "'," +
                "title='" + title + "'" +
                "}";
    }

    public static class Builder {
        private PleaValue pleaValue;

        private String title;

        public Builder withPleaValue(final PleaValue pleaValue) {
            this.pleaValue = pleaValue;
            return this;
        }

        public Builder withTitle(final String title) {
            this.title = title;
            return this;
        }

        public Offence build() {
            return new Offence(pleaValue, title);
        }
    }
}
