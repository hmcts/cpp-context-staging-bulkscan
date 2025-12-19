package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantCourtOptions;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantPlea;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(PleaDetailsUpdated.EVENT_NAME)
public class PleaDetailsUpdated implements Serializable {

    public static final String EVENT_NAME = "stagingbulkscan.events.plea-details-updated";

    private final UUID caseId;

    private final DefendantCourtOptions defendantCourtOptions;

    private final List<DefendantPlea> defendantPleas;

    @JsonCreator
    public PleaDetailsUpdated(@JsonProperty("caseId") final UUID caseId,
                              @JsonProperty("defendantCourtOptions") final DefendantCourtOptions defendantCourtOptions,
                              @JsonProperty("defendantPleas") final List<DefendantPlea> defendantPleas) {
        this.caseId = caseId;
        this.defendantCourtOptions = defendantCourtOptions;
        this.defendantPleas = defendantPleas;
    }

    public DefendantCourtOptions getDefendantCourtOptions() {
        return defendantCourtOptions;
    }

    public List<DefendantPlea> getDefendantPleas() {
        return defendantPleas;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
