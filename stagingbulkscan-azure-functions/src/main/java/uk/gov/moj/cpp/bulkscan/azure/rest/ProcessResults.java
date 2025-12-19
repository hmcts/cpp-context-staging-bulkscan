package uk.gov.moj.cpp.bulkscan.azure.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessResults {
    final List<AttachmentMetadata> attachmentMetadataList = new ArrayList<>();
    final List<String> urnNotFoundAttachments = new ArrayList<>();
    final List<String> prosecutorUrnNotMatchWithAttachments = new ArrayList<>();
    final List<String> nonPdfAttachments = new ArrayList<>();

    boolean emailWithNoAttachments = false;

    public void addAttachmentMetadata(AttachmentMetadata metadata) {
        attachmentMetadataList.add(metadata);
    }

    public void addNonPdfAttachment(String attachmentName) {
        nonPdfAttachments.add(attachmentName);
    }

    public void addUrnNotFoundAttachment(String attachmentName) {
        urnNotFoundAttachments.add(attachmentName);
    }

    public void addProsecutorUrnNotMatchWithAttachments(String attachmentName) {
        prosecutorUrnNotMatchWithAttachments.add(attachmentName);
    }

    public boolean hasInvalidAttachments() {
        return !urnNotFoundAttachments.isEmpty()
                || !nonPdfAttachments.isEmpty()
                || !prosecutorUrnNotMatchWithAttachments.isEmpty()
                || emailWithNoAttachments;
    }

    public List<String> getAllInvalidAttachments() {
        return Stream.of(
                nonPdfAttachments.stream(), urnNotFoundAttachments.stream(), prosecutorUrnNotMatchWithAttachments.stream())
                .flatMap(i -> i).collect(Collectors.toList());
    }

    public List<AttachmentMetadata> getAttachmentMetadataList() {
        return new ArrayList<>(attachmentMetadataList);
    }

    public List<String> getUrnNotFoundAttachments() {
        return new ArrayList<>(urnNotFoundAttachments);
    }

    public List<String> getProsecutorUrnNotMatchWithAttachments() {
        return new ArrayList<>(prosecutorUrnNotMatchWithAttachments);
    }

    public List<String> getNonPdfAttachments() {
        return new ArrayList<>(nonPdfAttachments);
    }

    public boolean isEmailWithNoAttachments() {
        return emailWithNoAttachments;
    }

    public void setEmailWithNoAttachments(final boolean emailWithNoAttachments) {
        this.emailWithNoAttachments = emailWithNoAttachments;
    }

}
