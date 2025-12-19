package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class ScannableItem {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("case_ptiurn")
    private String casePtiUrn;

    @JsonProperty("document_control_number")
    private String documentControlNumber;
    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("next_action")
    private String nextAction;

    @JsonProperty("next_action_date")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = DATE_FORMAT
    )
    private Date nextActionDate;

    @JsonProperty("prosecutor_ID")
    private String prosecutorId;

    @JsonProperty("scanning_date")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = DATE_FORMAT
    )
    private Date scanningDate;

    public ScannableItem(final String caseNumber, final String casePtiUrn, final String documentControlNumber,
                         final String documentName, final String fileName, final String nextAction,
                         final Date nextActionDate, final String prosecutorId, final Date scanningDate) {
        this.caseNumber = caseNumber;
        this.casePtiUrn = casePtiUrn;
        this.documentControlNumber = documentControlNumber;
        this.documentName = documentName;
        this.fileName = fileName;
        this.nextAction = nextAction;
        this.nextActionDate = new Date(nextActionDate.getTime());
        this.prosecutorId = prosecutorId;
        this.scanningDate = new Date(scanningDate.getTime());
    }

}
