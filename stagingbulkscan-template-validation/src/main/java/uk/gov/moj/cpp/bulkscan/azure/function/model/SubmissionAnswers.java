package uk.gov.moj.cpp.bulkscan.azure.function.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionAnswers implements Serializable {
    private static final long serialVersionUID = -7185076331018398831L;
    private final String email_address;

    public SubmissionAnswers(final String email_address) {
        this.email_address = email_address;
    }

    public String getEmail_address() {
        return email_address;
    }
}
