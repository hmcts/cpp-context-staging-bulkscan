package uk.gov.moj.cpp.stagingbulkscan.event.exception;

public class DocumentMissingException extends RuntimeException {

    public DocumentMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentMissingException(String message) {
        super(message);
    }
}
