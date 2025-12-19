package uk.gov.moj.cpp.bulkscan.azure.rest;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentMapper {

    private static final String SINGLE_PLEA = "Single Justice Procedure Notice - Plea (Single)";
    private static final String MULTIPLE_PLEA = "Single Justice Procedure Notice - Plea (Multiple)";
    private static final String MC100 = "SJPMC100";
    private static final String APPLICATION_FOR_EXTENSION_OF_PRECHARGE_BAIL = "Application for extension of precharge bail";
    private static final String INITIAL_DETAILS_PROS_CASE = "Initial Details Pros Case";
    private static final String RETURN_TO_SENDER_ENVELOPE = "return to sender envelope";
    private static final String SJPN = "SJPN";
    private static final String OTHER = "Other";
    private static final String CHARGES = "MG4E postal Requistion Adult Defendant";
    private static final String DISQUALIFICATION_NOTICE_RESPONSE = "Disqualification Notice Response";


    private Map<String, String> documentNameMap;

    private static DocumentMapper documentMapper;

    private DocumentMapper() {
        documentNameMap = new HashMap<>();
        documentNameMap.put(MC100, "FINANCIAL_MEANS");
        documentNameMap.put(SINGLE_PLEA, "PLEA");
        documentNameMap.put(MULTIPLE_PLEA, "PLEA");
        documentNameMap.put(APPLICATION_FOR_EXTENSION_OF_PRECHARGE_BAIL, "Applications");
        documentNameMap.put(INITIAL_DETAILS_PROS_CASE, "Case Summary");
        documentNameMap.put(RETURN_TO_SENDER_ENVELOPE, "OTHER-general correspondence");
        documentNameMap.put(SJPN, SJPN);
        documentNameMap.put(OTHER, "OTHER-general correspondence");
        documentNameMap.put(CHARGES, "Charges");
        documentNameMap.put(DISQUALIFICATION_NOTICE_RESPONSE, "DISQUALIFICATION_REPLY_SLIP");
    }

    public List<String> getSupportedDocuments() {
        return Collections.unmodifiableList(new ArrayList<>(documentNameMap.keySet()));
    }

    public String get(final String documentName) {
        return documentNameMap.get(documentName);
    }

    public String getSinglePlea() {
        return SINGLE_PLEA;
    }

    public String getMultiplePlea() {
        return MULTIPLE_PLEA;
    }

    public String getMc100() {
        return MC100;
    }

    public boolean isDocumentSupported(final String documentName) {
        return getSupportedDocuments().contains(documentName);
    }

    public static DocumentMapper getDocumentMapper() {
        if(isNull(documentMapper)) {
            documentMapper = new DocumentMapper();
            return documentMapper;
        }
        return documentMapper;
    }
}
