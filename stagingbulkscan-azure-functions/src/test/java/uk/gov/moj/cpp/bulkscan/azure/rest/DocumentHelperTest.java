package uk.gov.moj.cpp.bulkscan.azure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class DocumentHelperTest {

    private DocumentHelper documentHelper;

    @Mock
    private ReferenceDataQueryHelper referenceDataQueryHelper;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        documentHelper = new DocumentHelper(referenceDataQueryHelper);
    }

    @Test
    public void documentStatusShouldBePendingWithCaseURN() {
        final String caseUrn = "123456";
        final String casePTIUrn = null;
        final String prosecutorId = "qwe1234";
        final String documentName = "SJPMC100";
        final String status = documentHelper.determineDocumentStatus(caseUrn, casePTIUrn, prosecutorId, documentName);
        assertEquals("PENDING", status);
    }

    @Test
    public void documentStatusShouldBePendingWithCasePtiURN() {
        final String caseUrn = null;
        final String casePTIUrn = "123456";
        final String prosecutorId = null;
        final String documentName = "SJPMC100";
        when(referenceDataQueryHelper.getOuCodeByPtiUrn(any(String.class))).thenReturn(createObjectBuilder().add("oucode", "2345").build());
        final String status = documentHelper.determineDocumentStatus(caseUrn, casePTIUrn, prosecutorId, documentName);
        assertEquals("PENDING", status);
    }

    @Test
    public void documentStatusShouldBeFollowUpWhenCaseURNAndCasePTIUrnISNull() {
        final String caseUrn = null;
        final String casePTIUrn = null;
        final String prosecutorId = "qwe1234";
        final String documentName = "SJPMC100";
        final String status = documentHelper.determineDocumentStatus(caseUrn, casePTIUrn, prosecutorId, documentName);
        assertEquals("FOLLOW_UP", status);
    }

    @Test
    public void documentStatusShouldBeFollowUpWhenDocumentIsInValid() {
        final String caseUrn = "123456";
        final String casePTIUrn = null;
        final String prosecutorId = "qwe1234";
        final String documentName = "Unknown";
        final String status = documentHelper.determineDocumentStatus(caseUrn, casePTIUrn, prosecutorId, documentName);
        assertEquals("FOLLOW_UP", status);
    }
}
