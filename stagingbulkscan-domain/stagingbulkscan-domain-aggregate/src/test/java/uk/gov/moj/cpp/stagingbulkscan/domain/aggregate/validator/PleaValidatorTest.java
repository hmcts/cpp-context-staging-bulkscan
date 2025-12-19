package uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.stagingbulkscan.domain.PleaType.BOTH;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TITLE_INVALID;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TYPE_EMPTY;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.PLEA_TYPE_INVALID;
import static uk.gov.moj.cpp.stagingbulkscan.domain.aggregate.validator.ProblemCode.WISH_TO_COME_TO_COURT_INVALID;

import uk.gov.justice.stagingbulkscan.domain.Interpreter;
import uk.gov.justice.stagingbulkscan.domain.Interpreter.Builder;
import uk.gov.justice.stagingbulkscan.domain.Offence;
import uk.gov.justice.stagingbulkscan.domain.Plea;
import uk.gov.justice.stagingbulkscan.domain.PleaType;
import uk.gov.moj.cpp.stagingbulkscan.json.schemas.Problem;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PleaValidatorTest {

    @Test
    public void noProblemRaisedWithAllCorrectData() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(PleaType.GUILTY, true, "TV Licence was invalid", true, false),
                buildPleaWith(PleaType.GUILTY, true, "TV Licence was invalid", true, false), randomUUID());
        assertThat(problems.size(), is(0));
    }

    @Test
    public void emptyPleaType() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(null, true, "TV Licence was invalid", true, false),
                buildPleaWith(null, true, "TV Licence was invalid", true, false), randomUUID());
        assertThat(problems.size(), is(1));
        assertThat(problems.get(0).getCode(), is(PLEA_TYPE_EMPTY.name()));
    }

    @Test
    public void selectedBothPleaType() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(BOTH, true, "TV Licence was invalid", true, false),
                buildPleaWith(BOTH, true, "TV Licence was invalid", true, false), randomUUID());
        assertThat(problems.size(), is(1));
        assertThat(problems.get(0).getCode(), is(PLEA_TYPE_INVALID.name()));
    }

    @Test
    public void guiltyPleaWithNoOptionToComeToCourt() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(PleaType.GUILTY, null, "TV Licence was invalid", true, false),
                buildPleaWith(PleaType.GUILTY, null, "TV Licence was invalid", true, false), randomUUID());
        assertThat(problems.size(), is(1));
        assertThat(problems.get(0).getCode(), is(WISH_TO_COME_TO_COURT_INVALID.name()));
    }

    @Test
    public void emptyPleaTitle() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(PleaType.GUILTY, true, null, true, false),
                buildPleaWith(PleaType.GUILTY, true, "TV Licence not found", true, false), randomUUID());
        assertThat(problems.size(), is(1));
        assertThat(problems.get(0).getCode(), is(PLEA_TITLE_INVALID.name()));
    }

    @Test
    public void offenceHasFinaLDecision() {
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWith(PleaType.GUILTY, true, "TVLicencenotfound", true, false),
                buildPleaWith(PleaType.GUILTY, true, "TV Licence not found", true, true), randomUUID());
        assertThat(problems.size(), is(1));
        assertThat(problems.get(0).getCode(), is(ProblemCode.OFFENCE_HAS_FINAL_DECISION.name()));
    }

    @Test
    public void sameOffenceTitleMoreThanOnce(){
        final PleaValidator pleaValidator = new PleaValidator();
        final List<Problem> problems = pleaValidator.validate(buildPleaWithMultipleOffences(PleaType.GUILTY, true, "TV Licence not found", true, false),
                buildPleaWith(PleaType.GUILTY, true, "TV Licence not found", true, false), randomUUID());
        assertThat(problems.size(), is(2));
        assertThat(problems.get(0).getCode(), is(ProblemCode.SAME_OFFENCE_TITLE.name()));
        assertThat(problems.get(1).getCode(), is(ProblemCode.SAME_OFFENCE_TITLE.name()));
    }

    private Plea buildPleaWith(PleaType pleaType, Boolean wishToComeToCourt, String pleaTitle, Boolean welshHearing, boolean hasFinalDecision) {
        return new Plea(null, false, null, null, null, buildInterpreter(), buildOffences(pleaType, pleaTitle, hasFinalDecision), welshHearing, wishToComeToCourt);
    }

    private Plea buildPleaWithMultipleOffences(PleaType pleaType, Boolean wishToComeToCourt, String pleaTitle, Boolean welshHearing, boolean hasFinalDecision) {
        final List<Offence> offences = buildOffences(pleaType, pleaTitle, hasFinalDecision);
        offences.addAll(buildOffences(pleaType, pleaTitle, hasFinalDecision));
        return new Plea(null, false, null, null, null, buildInterpreter(), offences, welshHearing, wishToComeToCourt);
    }

    private Interpreter buildInterpreter() {
        final Builder interpreter = new Interpreter.Builder();
        interpreter.withLanguage("Welsh");
        interpreter.withNeeded(true);
        return interpreter.build();
    }

    private List<Offence> buildOffences(PleaType pleaType, String pleaTitle, boolean hasFinalDecision) {
        final List<Offence> offences = new ArrayList<>();
        offences.add(new Offence(hasFinalDecision, randomUUID(), pleaType, pleaTitle));
        return offences;
    }
}
