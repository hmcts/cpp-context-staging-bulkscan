package uk.gov.moj.cpp.stagingbulkscan.domain.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S00107"})
@Event("stagingbulkscan.events.scan-envelope-registered")
public class ScanEnvelopeRegistered implements Serializable {

    private final ScanEnvelope scanEnvelope;

    @JsonCreator
    public ScanEnvelopeRegistered(@JsonProperty("scanEnvelope") final ScanEnvelope scanEnvelope) {
        this.scanEnvelope = scanEnvelope;
    }

    public ScanEnvelope getScanEnvelope() {
        return scanEnvelope;
    }
}