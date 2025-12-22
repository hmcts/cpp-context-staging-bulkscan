package uk.gov.moj.cpp.bulkscan.azure.exception;

public class BulkScanProcessorException extends RuntimeException {

    public BulkScanProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public BulkScanProcessorException(String message) {
        super(message);
    }
}
