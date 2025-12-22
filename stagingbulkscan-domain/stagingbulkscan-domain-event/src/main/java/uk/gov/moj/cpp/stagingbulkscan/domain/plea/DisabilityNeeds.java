package uk.gov.moj.cpp.stagingbulkscan.domain.plea;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class DisabilityNeeds implements Serializable {

    @SuppressWarnings({"squid:S1700"})
    private final String disabilityNeeds;

    private final Boolean needed;

    public DisabilityNeeds(@JsonProperty("disabilityNeeds") final String disabilityNeeds,
                           @JsonProperty("needed") final Boolean needed) {
        this.disabilityNeeds = disabilityNeeds;
        this.needed = needed;
    }

    public String getDisabilityNeeds() {
        return disabilityNeeds;
    }

    public Boolean getNeeded() {
        return needed;
    }
}
