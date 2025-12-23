package uk.gov.moj.cpp.bulkscan.azure.function.util;

public class PageContent {
    private final int pageNumber;
    private final String text;

    public PageContent(int pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text != null ? text.trim() : "";
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getText() {
        return text;
    }
}
