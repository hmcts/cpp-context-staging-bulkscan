package uk.gov.moj.cpp.stagingbulkscan.domain.plea;

import uk.gov.justice.stagingbulkscan.domain.PleaType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DefendantPlea implements Serializable {

    private final UUID defendantId;

    private final UUID offenceId;

    private final PleaType pleaType;

    private final Boolean wishToComeToCourt;

    @JsonCreator
    public DefendantPlea(@JsonProperty("defendantId") final UUID defendantId,
                         @JsonProperty("offenceId") final UUID offenceId,
                         @JsonProperty("pleaType") final PleaType pleaType,
                         @JsonProperty("wishToComeToCourt") final Boolean wishToComeToCourt) {
        this.defendantId = defendantId;
        this.offenceId = offenceId;
        this.pleaType = pleaType;
        this.wishToComeToCourt = wishToComeToCourt;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public PleaType getPleaType() {
        return pleaType;
    }

    public Boolean getWishToComeToCourt() {
        return wishToComeToCourt;
    }
}
