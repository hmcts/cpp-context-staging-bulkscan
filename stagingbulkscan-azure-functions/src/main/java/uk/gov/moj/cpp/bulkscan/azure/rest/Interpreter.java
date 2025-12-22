package uk.gov.moj.cpp.bulkscan.azure.rest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Interpreter implements Serializable {
    private static final long serialVersionUID = -2287352870561308209L;

    private final String language;

    private final Boolean needed;

    public Interpreter(final String language, final Boolean needed) {
        this.language = language;
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getNeeded() {
        return needed;
    }

    public static Builder interpreter() {
        return new Interpreter.Builder();
    }

    @Override
    public String toString() {
        return "Interpreter{" +
                "language='" + language + "'," +
                "needed='" + needed + "'" +
                "}";
    }

    public static class Builder {
        private String language;

        private Boolean needed;

        public Builder withLanguage(final String language) {
            this.language = language;
            return this;
        }

        public Builder withNeeded(final Boolean needed) {
            this.needed = needed;
            return this;
        }

        public Interpreter build() {
            return new Interpreter(language, needed);
        }
    }
}
