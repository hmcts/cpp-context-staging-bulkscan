package uk.gov.moj.cpp.stagingbulkscan.domain.plea;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefendantCourtOptions implements Serializable {

    private final boolean welshHearing;
    private final Interpreter interpreter;
    private DisabilityNeeds disabilityNeeds;

    @JsonCreator
    public DefendantCourtOptions(@JsonProperty("welshHearing") final boolean welshHearing,
                                 @JsonProperty("interpreter") final Interpreter interpreter,
                                 @JsonProperty("disabilityNeeds") final DisabilityNeeds disabilityNeeds) {
        this.welshHearing = welshHearing;
        this.interpreter = interpreter;
        this.disabilityNeeds = disabilityNeeds;
    }

    public boolean isWelshHearing() {
        return welshHearing;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public DisabilityNeeds getDisabilityNeeds() {
        return disabilityNeeds;
    }
}
