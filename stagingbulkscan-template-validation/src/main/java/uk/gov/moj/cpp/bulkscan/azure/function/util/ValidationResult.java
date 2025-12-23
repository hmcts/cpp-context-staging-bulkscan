package uk.gov.moj.cpp.bulkscan.azure.function.util;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> details = new ArrayList<>();
    private String documentType;

    public void addError(String error) {
        errors.add(error);
    }

    public void addDetail(String detail) {
        details.add(detail);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getDetails() {
        return new ArrayList<>(details);
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
}
