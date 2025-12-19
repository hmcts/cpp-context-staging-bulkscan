package uk.gov.moj.cpp.bulkscan.azure.exception;

public class InvalidZipPayloadException extends RuntimeException {

    public InvalidZipPayloadException(String message) {
        super(message);
    }

    public InvalidZipPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
