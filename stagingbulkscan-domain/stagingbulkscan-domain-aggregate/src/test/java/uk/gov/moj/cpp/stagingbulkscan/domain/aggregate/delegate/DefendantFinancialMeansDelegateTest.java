package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.stagingbulkscan.domain.AllFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.DocumentFinancialMeans;
import uk.gov.justice.stagingbulkscan.domain.EmploymentStatus;
import uk.gov.justice.stagingbulkscan.domain.Frequency;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantFinancialMeansUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantFinancialMeansDelegateTest {
    private static final UUID SCAN_DOCUMENT_ID = randomUUID();
    private static final UUID ACTIONED_BY = randomUUID();
    private static final String NI_NUMBER = "AB123456C";
    private static final String ZIP_FILE_NAME = "ABC.zip";
    private static final String PDF_NAME = "XYZ.pdf";
    private static final String CASE_URN = "CASE_URN";
    private static final String DOC_NAME = "DOC_NAME";
    private static final String PROSECUTOR_AUTHORITY_ID = "SJP";

    private DefendantFinancialMeansDelegate defendantFinancialMeansDelegate;

    private StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    @BeforeEach
    public void setup() {
        stagingBulkScanAggregateMemento = new StagingBulkScanAggregateMemento();
        defendantFinancialMeansDelegate  = new DefendantFinancialMeansDelegate(stagingBulkScanAggregateMemento);
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForValidNiNumber() {

        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withNiNumber("AB123456C")
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .withAverageIncome("")
                .withFrequencyFortnightly(false)
                .withFrequencyMonthly(false)
                .withFrequencyWeekly(false)
                .withFrequencyYearly(false)
                .withNameOfOrganization("")
                .withNoIncome(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("nationalInsuranceNumber", is(NI_NUMBER)),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForValidIncomeDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withAverageIncome("12345")
                .withFrequencyFortnightly(false)
                .withFrequencyMonthly(true)
                .withFrequencyWeekly(false)
                .withFrequencyYearly(false)
                .withNameOfOrganization("")
                .withNoIncome(false)
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("amount", is(new BigDecimal(12345)))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("frequency", is(Frequency.MONTHLY))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForYearlyIncomeDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withAverageIncome("12345")
                .withFrequencyFortnightly(false)
                .withFrequencyMonthly(false)
                .withFrequencyWeekly(false)
                .withFrequencyYearly(true)
                .withNameOfOrganization("")
                .withNoIncome(false)
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("amount", is(new BigDecimal(12345)))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("frequency", is(Frequency.YEARLY))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForValidEmploymentDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withEmployed(true)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employment", hasProperty("status", is(EmploymentStatus.EMPLOYED))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForSelfEmploymentDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withEmployed(false)
                .withSelfEmployed(true)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employment", hasProperty("status", is(EmploymentStatus.SELF_EMPLOYED))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForUnEmploymentDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(true)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employment", hasProperty("status", is(EmploymentStatus.UNEMPLOYED))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForOtherEmploymentDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(true)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employment", hasProperty("status", is(EmploymentStatus.OTHER))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForValidEmployerDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();

        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withNameOfOrganization("employerName")
                .withEmployerAddressLine1("addressLine1")
                .withEmployerAddressPostCode("EC1A1BB")
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employer", hasProperty("address", hasProperty("address1", is("addressLine1")))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employer", hasProperty("address", hasProperty("postcode", is("EC1A1BB")))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("employer", hasProperty("name", is("employerName"))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForValidBenefitsDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();

        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withClaimBenefits(true)
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("benefits", hasProperty("claimed", is(true))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedAndFollowUpEventsForInValidIncomeDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withAverageIncome("12345")
                .withFrequencyFortnightly(false)
                .withFrequencyMonthly(true)
                .withFrequencyWeekly(false)
                .withFrequencyYearly(false)
                .withNameOfOrganization("")
                .withNoIncome(true)
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("amount")),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("frequency")),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(ScanDocumentFollowedUp.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("actionedBy", is(ACTIONED_BY)),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    @Test
    public void shouldRaiseFinancialMeansUpdatedEventForNoIncomeDetails() {
        final AllFinancialMeans allFinancialMeans = AllFinancialMeans.allFinancialMeans()
                .withScanDocumentId(SCAN_DOCUMENT_ID)
                .build();
        final DocumentFinancialMeans documentFinancialMeans = DocumentFinancialMeans.documentFinancialMeans()
                .withAverageIncome("")
                .withFrequencyFortnightly(false)
                .withFrequencyMonthly(false)
                .withFrequencyWeekly(false)
                .withFrequencyYearly(false)
                .withNameOfOrganization("")
                .withNoIncome(true)
                .withEmployed(false)
                .withSelfEmployed(false)
                .withUnemployed(false)
                .withOtherEmploymentStatus(false)
                .build();

        stagingBulkScanAggregateMemento.registerScanEnvelope(buildPayload(documentFinancialMeans));

        final Stream<Object> eventStream = defendantFinancialMeansDelegate.updateDefendantFinancialMeans(allFinancialMeans, ACTIONED_BY);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DefendantFinancialMeansUpdated.class),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("income", hasProperty("amount", is(new BigDecimal(0)))),
                Matchers.<DefendantFinancialMeansUpdated>hasProperty("scanDocumentId", is(SCAN_DOCUMENT_ID))
        )));
    }

    private ScanEnvelope buildPayload(final DocumentFinancialMeans documentFinancialMeans) {
        return ScanEnvelope.scanEnvelope()
                .withAssociatedScanDocuments(
                        Collections.singletonList(ScanDocument.scanDocument()
                                .withScanDocumentId(SCAN_DOCUMENT_ID)
                                .withCaseUrn(CASE_URN)
                                .withDocumentName(DOC_NAME)
                                .withFileName(PDF_NAME)
                                .withProsecutorAuthorityId(PROSECUTOR_AUTHORITY_ID)
                                .withVendorReceivedDate(ZonedDateTime.now())
                                .withScanningDate(ZonedDateTime.now())
                                .withMc100s(documentFinancialMeans)
                                .build()))
                .withScanEnvelopeId(randomUUID())
                .withExtractedDate(ZonedDateTime.now())
                .withZipFileCreatedDate(ZonedDateTime.now())
                .withZipFileName(ZIP_FILE_NAME)
                .build();
    }
}
