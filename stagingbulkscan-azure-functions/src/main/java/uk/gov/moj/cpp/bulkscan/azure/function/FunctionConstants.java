package uk.gov.moj.cpp.bulkscan.azure.function;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

public interface FunctionConstants {

    // Police Email Extractor Function constants
    String URN_REGEX = "\\d{2}[/\\s]?[a-zA-Z0-9]{2}[/\\s]?\\d{5}[/\\s]?\\d{2}";
    String POLICE_EMAIL_EXTRACTOR_FUNCTION = "policeEmailExtractorFunction";

    String APPLICATION_PDF = "application/pdf";
    Pattern fromSentPattern = Pattern.compile("From:(.*?)Sent");
    @SuppressWarnings({"java:S5998"})
    Pattern emailPattern = Pattern.compile("([_A-Za-z0-9-]+)(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})");
    String OUCODE = "oucode";
    String DOCUMENT_CONTROL_NUMBER_DATE_FORMAT = "HH:mm:ss.SSS'Z'";
    String ZIP_FILE_NAME_DATE_FORMAT = "dd-MM-yyyy-HH-mm-ss";
    String ZIP_EXTENSION = ".zip";
    String PDF_EXTENSION = ".pdf";
    String JSON_EXTENSION = ".json";
    String ENVIRONMENT = "environment";

    AtomicInteger documentControlNoSeq = new AtomicInteger(1000);
    IntUnaryOperator resetOrIncrement = currentValue -> currentValue == 9999 ? 1000 : currentValue + 1;

    static String createDocumentControlNumber() {
        final int sequence = documentControlNoSeq.updateAndGet(resetOrIncrement);

        return sequence +
                formatTimestamp(DOCUMENT_CONTROL_NUMBER_DATE_FORMAT).replaceAll("\\D+", "");
    }

    static String formatTimestamp(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }
}
