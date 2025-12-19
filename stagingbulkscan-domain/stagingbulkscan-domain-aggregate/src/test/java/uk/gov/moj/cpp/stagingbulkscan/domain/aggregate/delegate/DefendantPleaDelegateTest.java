package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.Lists;
import uk.gov.justice.stagingbulkscan.domain.Address;
import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.Gender;
import uk.gov.justice.stagingbulkscan.domain.Interpreter;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.justice.stagingbulkscan.domain.ScanEnvelope;
import uk.gov.justice.stagingbulkscan.domain.Title;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantPlea;

public class DefendantPleaDelegateTest {

    private static final UUID ACTIONED_BY = randomUUID();

    private DefendantPleaDelegate defendantPleaDelegate;

    private StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    private static final UUID DOCUMENT_ID_1 = randomUUID();
    private static final UUID DOCUMENT_ID_2 = randomUUID();
    private static final String ZIP_FILE_NAME = "ABC.zip";
    private static final String PDF_NAME = "XYZ.pdf";

    @BeforeEach
    public void setup() {
        stagingBulkScanAggregateMemento = new StagingBulkScanAggregateMemento();
        defendantPleaDelegate = new DefendantPleaDelegate(stagingBulkScanAggregateMemento);
    }

    @Test
    public void updateDefendantDetails() {

        final Defendant defendant = getDefendant(Plea.plea().withWelshHearing(false).withOffences(singleSjpOffence()).build());
        final String drivingLicenceNumber = "RAMAS808090P99GW";
        stagingBulkScanAggregateMemento.registerScanEnvelope(getScanEnvelopeWithSamePleaValue(drivingLicenceNumber));

        final Stream<Object> eventsStream = defendantPleaDelegate.updateDefendantDetails(defendant, ACTIONED_BY);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(2));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        assertThat(events.get(1), instanceOf(PleaDetailsUpdated.class));
    }

    @Test
    public void shouldNotCheckOffenceTitleWithSamePleaValueForAllOffences() {

        final Defendant defendant = getDefendant(Plea.plea().withWelshHearing(false).withOffences(multipleSjpOffences()).build());
        final String drivingLicenceNumber = "RAMAS808090P99GW";
        stagingBulkScanAggregateMemento.registerScanEnvelope(getScanEnvelopeWithSamePleaValue(drivingLicenceNumber));

        final Stream<Object> eventsStream = defendantPleaDelegate.updateDefendantDetails(defendant, ACTIONED_BY);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(2));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        assertThat(events.get(1), instanceOf(PleaDetailsUpdated.class));
        final List<DefendantPlea> defendantPleas = ((PleaDetailsUpdated) events.get(1)).getDefendantPleas();
        assertThat(defendantPleas.get(0).getPleaType(), is(PleaType.NOT_GUILTY));
        assertThat(defendantPleas.get(1).getPleaType(), is(PleaType.NOT_GUILTY));
    }

    @Test
    public void shouldNotUpdatePleaWhenOffenceTitleIsNotMatchingWithMoreThanOneOffence() {

        final Defendant defendant = getDefendant(Plea.plea().withWelshHearing(false).withOffences(multipleSjpOffences()).build());
        final String drivingLicenceNumber = "RAMAS808090P99GW";
        stagingBulkScanAggregateMemento.registerScanEnvelope(getScanEnvelopeWithDifferentPleaValue(drivingLicenceNumber));

        final Stream<Object> eventsStream = defendantPleaDelegate.updateDefendantDetails(defendant, ACTIONED_BY);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(2));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        assertThat(events.get(1), instanceOf(PleaDetailsUpdated.class));
        final List<DefendantPlea> defendantPleas = ((PleaDetailsUpdated) events.get(1)).getDefendantPleas();
        assertThat(defendantPleas.size(), is(1));
        assertThat(defendantPleas.get(0).getPleaType(), is(PleaType.NOT_GUILTY));
    }

    @Test
    public void shouldNotCheckOffenceTitleWhenThereIsOnlyOneOffence() {

        final Defendant defendant = getDefendant(Plea.plea().withWelshHearing(false).withOffences(singleSjpOffence()).build());
        final String drivingLicenceNumber = "RAMAS808090P99GW";
        stagingBulkScanAggregateMemento.registerScanEnvelope(getScanEnvelopeWithSamePleaValue(drivingLicenceNumber));

        final Stream<Object> eventsStream = defendantPleaDelegate.updateDefendantDetails(defendant, ACTIONED_BY);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(2));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        assertThat(events.get(1), instanceOf(PleaDetailsUpdated.class));
        final List<DefendantPlea> defendantPleas = ((PleaDetailsUpdated) events.get(1)).getDefendantPleas();
        assertThat(defendantPleas.get(0).getPleaType(), is(PleaType.NOT_GUILTY));
    }

    @Test
    public void updateDefendantDetailsAndSendToExceptionQueue_whenDrivingLicenseNumberIsDifferentThanExisting() {
        final String existingDrivingLicenceNumber = "RAMAS818090P99GW";
        final Defendant defendant = getDefendant(Plea.plea().withWelshHearing(false)
                .withOffences(singleSjpOffence()).withDrivingLicenceNumber(existingDrivingLicenceNumber)
                .build());
        final String drivingLicenceNumber = "RAMAS878090P99GW";
        stagingBulkScanAggregateMemento.registerScanEnvelope(getScanEnvelopeWithSamePleaValue(drivingLicenceNumber));

        final Stream<Object> eventsStream = defendantPleaDelegate.updateDefendantDetails(defendant, ACTIONED_BY);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(3));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        assertThat(events.get(1), instanceOf(ScanDocumentFollowedUp.class));
        assertThat(events.get(2), instanceOf(PleaDetailsUpdated.class));
    }

    private ScanEnvelope getScanEnvelopeWithSamePleaValue(final String drivingLicenceNumber) {

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                        .withId(randomUUID())
                        .withTitle("Offence title 1")
                        .withHasFinalDecision(false)
                        .withPleaValue(PleaType.NOT_GUILTY).build());

        return getScanEnvelope(drivingLicenceNumber, offences);
    }

    private ScanEnvelope getScanEnvelopeWithDifferentPleaValue(final String drivingLicenceNumber) {

        final List<Offence> offences = new ArrayList<>();
        offences.add(Offence.offence()
                        .withId(randomUUID())
                        .withTitle("Possessatelevisionsetwithinten")
                        .withHasFinalDecision(false)
                        .withPleaValue(PleaType.NOT_GUILTY).build());
        offences.add(
                Offence.offence()
                        .withId(randomUUID())
                        .withTitle("Offence title 2")
                        .withHasFinalDecision(false)
                        .withPleaValue(PleaType.GUILTY).build());

        return getScanEnvelope(drivingLicenceNumber, offences);
    }

    private ScanEnvelope getScanEnvelope(String drivingLicenceNumber, List<Offence> offences) {
        return ScanEnvelope.scanEnvelope()
                .withZipFileName(ZIP_FILE_NAME)
                .withAssociatedScanDocuments(
                        Arrays.asList(ScanDocument.scanDocument()
                                        .withScanDocumentId(DOCUMENT_ID_1)
                                        .withPlea(Plea.plea()
                                                .withInterpreter(Interpreter.interpreter().withLanguage("welsh").withNeeded(false).build())
                                                .withEmailAddress("jsmith@hmcts.net")
                                                .withDetailsCorrect(true)
                                                .withWishToComeToCourt(false)
                                                .withWelshHearing(false)
                                                .withOffences(offences)
                                                .withDrivingLicenceNumber(drivingLicenceNumber)
                                                .build())
                                        .withFileName(PDF_NAME)
                                        .build(),
                                ScanDocument.scanDocument()
                                        .withScanDocumentId(DOCUMENT_ID_2)
                                        .withFileName(PDF_NAME)
                                        .build())).build();
    }

    private Defendant getDefendant(final Plea plea) {
        return Defendant.defendant()
                .withCaseId(randomUUID())
                .withId(randomUUID())
                .withTitle(Title.MR)
                .withFirstName("firstName")
                .withLastName("lastName")
                .withDateOfBirth("01-01-1977")
                .withGender(Gender.MALE)
                .withEmail("someone@domain.com")
                .withAddress(Address.address()
                        .withAddress1("Flat 1, Armageddon House")
                        .withAddress2("13 Old Road")
                        .withAddress3("Giggleswick")
                        .withAddress4("Merton")
                        .withAddress5("London")
                        .withPostcode("DN35 1AB").build())
                .withContactDetails(ContactDetails.contactDetails().build())
                .withScanEnvelopeId(randomUUID())
                .withScanDocumentId(DOCUMENT_ID_1)
                .withNationalInsuranceNumber("AE123456C")
                .withPlea(plea)
                .build();
    }

    private List<Offence> singleSjpOffence() {
        return Lists.newArrayList(Offence.offence()
                .withId(randomUUID())
                .withTitle("Possess a television set with intent to install / use without a licence")
                .withHasFinalDecision(false)
                .withPleaValue(PleaType.GUILTY).build());
    }
    private List<Offence> multipleSjpOffences() {
        return Lists.newArrayList(Offence.offence()
                .withId(randomUUID())
                .withTitle("Possess a television set with intent to install / use without a licence")
                .withHasFinalDecision(false)
                .withPleaValue(PleaType.GUILTY).build(),
                Offence.offence()
                        .withId(randomUUID())
                        .withTitle("Fail to make good damage / remove obstruction from footpath / bridleway before cessation of a diversion")
                        .withHasFinalDecision(false)
                        .withPleaValue(PleaType.GUILTY).build());
    }
}
