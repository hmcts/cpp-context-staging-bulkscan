package uk.gov.moj.cpp.stagingbulkscan.domain.plea;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Interpreter implements Serializable {

    private final String language;

    private final Boolean needed;

    @JsonCreator
    public Interpreter(@JsonProperty("language") final String language,
                       @JsonProperty("needed") final Boolean needed) {
        this.language = language;
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getNeeded() {
        return needed;
    }
}

