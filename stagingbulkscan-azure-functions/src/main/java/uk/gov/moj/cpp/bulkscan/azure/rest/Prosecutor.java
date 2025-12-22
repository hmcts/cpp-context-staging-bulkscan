package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

public class Prosecutor {
    private String emailAddress;
    private Set<String> authorityCodes;

    public Prosecutor(final String emailAddress, final Set<String> authorityCodes) {
        this.authorityCodes = new HashSet<>(authorityCodes);
        this.emailAddress = emailAddress;
    }


    public Set<String> getAuthorityCodes() {
        return new HashSet<>(authorityCodes);
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
