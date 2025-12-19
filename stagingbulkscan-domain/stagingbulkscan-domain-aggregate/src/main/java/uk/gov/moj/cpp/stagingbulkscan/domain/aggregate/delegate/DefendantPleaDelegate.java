package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.empty;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.stagingbulkscan.domain.ContactDetails;
import uk.gov.justice.stagingbulkscan.domain.Defendant;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.ScanDocument;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.DefendantDetailsRequestValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.PleaValidator;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.PleaDetailsUpdated;
import uk.gov.moj.cpp.stagingbulkscan.domain.event.ScanDocumentFollowedUp;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantCourtOptions;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DefendantPlea;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.DisabilityNeeds;
import uk.gov.moj.cpp.stagingbulkscan.domain.plea.Interpreter;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendantPleaDelegate implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantPleaDelegate.class);

    private static final long serialVersionUID = 1L;

    public static final int MAX_OFFENCE_TITLE_LENGTH = 30;

    private final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento;

    public DefendantPleaDelegate(final StagingBulkScanAggregateMemento stagingBulkScanAggregateMemento) {
        this.stagingBulkScanAggregateMemento = stagingBulkScanAggregateMemento;
    }

    public Stream<Object> updateDefendantDetails(final Defendant defendantDetails, final UUID actionedBy) {
        final UUID scanDocumentId = defendantDetails.getScanDocumentId();
        final Optional<ScanDocument> scanDocument = this.stagingBulkScanAggregateMemento.getScanEnvelope().getAssociatedScanDocuments().stream()
                .filter(document -> document.getScanDocumentId().equals(scanDocumentId))
                .findFirst();

        final Stream.Builder<Object> streamBuilder = Stream.builder();

        if (!scanDocument.isPresent()) {
            LOGGER.error("Scan Document not found when processing defendant details in aggregator");
            return empty();
        }

        final Plea pleaInfo = scanDocument.get().getPlea();

        if (pleaInfo == null) {
            LOGGER.error("Plea information not available while trying to update defendant details");
            return empty();
        }

        final UUID scanEnvelopeId = this.stagingBulkScanAggregateMemento.getScanEnvelope().getScanEnvelopeId();

        if (!pleaInfo.getDetailsCorrect()) {
            final ScanDocumentFollowedUp scanDocumentFollowedUp = new ScanDocumentFollowedUp(scanEnvelopeId, scanDocumentId, actionedBy, now(UTC));
            streamBuilder.add(scanDocumentFollowedUp);
        }

        final DefendantDetailsRequestValidator defendantDetailsRequestValidator = new DefendantDetailsRequestValidator(
                defendantDetails.getEmail(),
                pleaInfo.getEmailAddress(),
                defendantDetails.getContactDetails().getMobile(),
                pleaInfo.getContactNumber(),
                defendantDetails.getDriverNumber(),
                pleaInfo.getDrivingLicenceNumber());


        if (defendantDetailsRequestValidator.isDefendantDetailsUpdated()) {
            streamBuilder.add(createDefendantDetailsUpdated(defendantDetailsRequestValidator.getEmail(), defendantDetailsRequestValidator.getPhoneNumber(), defendantDetails, defendantDetailsRequestValidator.getDrivingLicenseNumber()));

        }
        if (!defendantDetailsRequestValidator.isDrivingLicenseNumberValid() || defendantDetailsRequestValidator.isDrivingLicenseNumberMismatch()) {
            streamBuilder.add(new ScanDocumentFollowedUp(scanEnvelopeId, scanDocument.get().getScanDocumentId(), actionedBy, now(UTC)));
        }

        final List<Problem> problems = new PleaValidator().validate(pleaInfo, defendantDetails.getPlea(), scanDocument.get().getScanDocumentId());
        if (problems.isEmpty()) {
            streamBuilder.add(updatePleaDetails(defendantDetails, pleaInfo));
        } else {
            streamBuilder.add(new ScanDocumentFollowedUp(scanEnvelopeId, scanDocument.get().getScanDocumentId(), actionedBy, now(UTC)));
        }

        return streamBuilder.build();
    }

    private PleaDetailsUpdated updatePleaDetails(final Defendant defendantDetails, final Plea pl) {
        final List<DefendantPlea> defendantPleas = new ArrayList<>();
        final List<Offence> offences = pl.getOffences();

        final Interpreter interpreter = ofNullable(pl.getInterpreter())
                .map(i -> new Interpreter(pl.getInterpreter().getLanguage(), pl.getInterpreter().getNeeded()))
                .orElseGet(() -> Optional.ofNullable(defendantDetails.getPlea().getInterpreter()).map(p -> new Interpreter(p.getLanguage(), p.getNeeded())).orElse(null));

        final DisabilityNeeds disabilityNeeds = ofNullable(defendantDetails.getPlea().getDisabilityNeeds()).map(disability -> new DisabilityNeeds(disability.getDisabilityNeeds(), disability.getNeeded()))
                .orElse(null);

        final DefendantCourtOptions defendantCourtOptions = ofNullable(pl.getWelshHearing())
                .map(welshHearing -> new DefendantCourtOptions(welshHearing, interpreter, disabilityNeeds))
                .orElse(new DefendantCourtOptions(defendantDetails.getPlea().getWelshHearing(), interpreter, disabilityNeeds));

        final List<Offence> cleanedOffenceList = defendantDetails.getPlea().getOffences().stream().map(offence -> Offence.offence().withId(offence.getId())
                .withTitle(cleanOffenceTitle(offence.getTitle()))
                .withHasFinalDecision(offence.getHasFinalDecision())
                .withPleaValue(offence.getPleaValue()).build()).collect(Collectors.toList());

        final boolean allPleaValuesAreSame = offences.stream().map(Offence::getPleaValue).distinct().count() <= 1;

        cleanedOffenceList.forEach(offence -> {
            final Optional<Offence> matchedOffence = offences.stream().filter(off -> allPleaValuesAreSame|| off.getTitle().equalsIgnoreCase(offence.getTitle()))
                    .findFirst();
            matchedOffence.ifPresent(matched -> {
                if (offence.getPleaValue() != matched.getPleaValue()) {
                    defendantPleas.add(new DefendantPlea(defendantDetails.getId(), offence.getId(), matched.getPleaValue(), pl.getWishToComeToCourt()));
                } else {
                    defendantPleas.add(new DefendantPlea(defendantDetails.getId(), offence.getId(), offence.getPleaValue(), pl.getWishToComeToCourt()));
                }
            });
        });

        return new PleaDetailsUpdated(defendantDetails.getCaseId(), defendantCourtOptions, defendantPleas);
    }

    private String cleanOffenceTitle(final String offenceTitle) {
        final String whitespaceRemovedTitle = StringUtils.deleteWhitespace(offenceTitle);
        return whitespaceRemovedTitle.length() <=MAX_OFFENCE_TITLE_LENGTH ? whitespaceRemovedTitle : whitespaceRemovedTitle.substring(0, MAX_OFFENCE_TITLE_LENGTH);
    }

    private DefendantDetailsUpdated createDefendantDetailsUpdated(final String email, final String mobileNumber, final Defendant defendantDetails, final String drivingLicenseNumber) {

        final ContactDetails contactDetails = new ContactDetails(
                defendantDetails.getContactDetails().getBusiness(),
                defendantDetails.getContactDetails().getEmail(),
                defendantDetails.getContactDetails().getEmail2(),
                defendantDetails.getContactDetails().getHome(),
                mobileNumber);

        return new DefendantDetailsUpdated(
                defendantDetails.getScanDocumentId(),
                defendantDetails.getCaseId(),
                defendantDetails.getScanEnvelopeId(),
                defendantDetails.getId(),
                defendantDetails.getTitle(),
                defendantDetails.getFirstName(),
                defendantDetails.getLastName(),
                defendantDetails.getDateOfBirth(),
                defendantDetails.getGender(),
                email,
                defendantDetails.getNationalInsuranceNumber(),
                contactDetails,
                defendantDetails.getAddress(),
                drivingLicenseNumber
        );
    }
}
