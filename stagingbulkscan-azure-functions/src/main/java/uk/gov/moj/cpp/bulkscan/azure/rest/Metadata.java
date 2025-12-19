package uk.gov.moj.cpp.bulkscan.azure.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Metadata {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @JsonProperty("scannable_items")
    private List<ScannableItem> scannableItems;
    @JsonProperty("jurisdiction")
    private String jurisdiction;
    @JsonProperty("opening_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date openingDate;
    @JsonProperty("po_box")
    private String poBox;
    @JsonProperty("zip_file_createddate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date zipFileCreatedDate;
    @JsonProperty("zip_file_name")
    private String zipFileName;
    @JsonProperty("delivery_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_FORMAT)
    private Date deliveryDate;

    public Metadata(final List<ScannableItem> scannableItems, final String jurisdiction,
             final Date openingDate, final String poBox, final Date zipFileCreatedDate,
             final String zipFileName, final Date deliveryDate) {
        this.scannableItems = new ArrayList<>(scannableItems);
        this.jurisdiction = jurisdiction;
        this.openingDate = new Date(openingDate.getTime());
        this.poBox = poBox;
        this.zipFileCreatedDate = new Date(zipFileCreatedDate.getTime());
        this.zipFileName = zipFileName;
        this.deliveryDate = new Date(deliveryDate.getTime());
    }

}
