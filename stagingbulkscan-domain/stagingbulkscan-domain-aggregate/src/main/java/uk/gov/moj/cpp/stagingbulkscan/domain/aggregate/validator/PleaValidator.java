package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.stagingbulkscan.domain.PleaType.GUILTY;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.delegate.DefendantPleaDelegate.MAX_OFFENCE_TITLE_LENGTH;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.OFFENCE_HAS_FINAL_DECISION;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TITLE_INVALID;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TYPE_EMPTY;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TYPE_INVALID;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.SAME_OFFENCE_TITLE;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.WISH_TO_COME_TO_COURT_INVALID;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.ProblemValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class PleaValidator {

    @SuppressWarnings("squid:S1188")
    //Message: Reduce this lambda expression number of lines from 29 to at most 20.
    public List<Problem> validate(final Plea pl, Plea sjpPlea, UUID scanDocumentId) {
        final List<Problem> problems = new ArrayList<>();
        pl.getOffences().forEach(offence -> {
            if (PleaType.BOTH.equals(offence.getPleaValue())) {
                final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("PLEA_TYPE").withValue(PleaType.BOTH.toString()).build()).collect(toList());
                problems.add(new Problem(PLEA_TYPE_INVALID.toString(), problemValues));
            }
            if (offence.getPleaValue() == null) {
                final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("PLEA_TYPE").withValue("EMPTY").build()).collect(toList());
                problems.add(new Problem(PLEA_TYPE_EMPTY.toString(), problemValues));
            }
            if (isBlank(offence.getTitle())) {
                final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("PLEA_TITLE").withValue(offence.getTitle()).build()).collect(toList());
                problems.add(new Problem(PLEA_TITLE_INVALID.toString(), problemValues));
            }
            problems.addAll(sjpPlea.getOffences().stream().filter(i -> cleanOffenceTitle(i.getTitle()).equalsIgnoreCase(offence.getTitle())).filter(Offence::getHasFinalDecision).map(j -> {
                final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("OFFENCE_HAS_FINAL_DECISION").withValue(offence.getTitle()).build()).collect(toList());
                return new Problem(OFFENCE_HAS_FINAL_DECISION.toString(), problemValues);
            }).collect(toList()));

            final Long numberOfSameOffenceTitle = pl.getOffences().stream().filter(i -> i.getTitle() != null && i.getTitle().equalsIgnoreCase(offence.getTitle())).count();
            if(numberOfSameOffenceTitle > 1){
                final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("SAME_OFFENCE_TITLE").withValue(offence.getTitle()).build()).collect(toList());
                problems.add(new Problem(SAME_OFFENCE_TITLE.toString(), problemValues));
            }

        });

        problems.addAll(pl.getOffences().stream().filter(plea -> GUILTY.equals(plea.getPleaValue())).filter(plea -> pl.getWishToComeToCourt() == null).map(plea -> {
            final List<ProblemValue> problemValues = Stream.of(new ProblemValue.Builder().withId(scanDocumentId.toString()).withKey("WISH_TO_COME_TO_COURT").withValue("INVALID").build()).collect(toList());
            return new Problem(WISH_TO_COME_TO_COURT_INVALID.toString(), problemValues);
        }).collect(toList()));

        return problems;
    }

    private String cleanOffenceTitle(final String offenceTitle) {
        final String whitespaceRemovedTitle = StringUtils.deleteWhitespace(offenceTitle);
        return whitespaceRemovedTitle.length() <= MAX_OFFENCE_TITLE_LENGTH ? whitespaceRemovedTitle : whitespaceRemovedTitle.substring(0, MAX_OFFENCE_TITLE_LENGTH);
    }
}
