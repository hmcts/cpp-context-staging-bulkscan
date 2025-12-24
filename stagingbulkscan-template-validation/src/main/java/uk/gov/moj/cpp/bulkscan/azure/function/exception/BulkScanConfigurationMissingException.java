package uk.gov.moj.cpp.bulkscan.azure.function.exception;

public class BulkScanConfigurationMissingException extends RuntimeException {

    public BulkScanConfigurationMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BulkScanConfigurationMissingException(String message) {
        super(message);
    }
}
