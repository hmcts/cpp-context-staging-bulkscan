package uk.gov.moj.cpp.bulkscan.azure.rest;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

public class PleaBuilderTest {

    @Test
    public void defendantPersonalDetailsAreUpdatedCorrectly() {
        PleaBuilder pleaBuilder = new PleaBuilder();
        final ImmutableMap.Builder<String, String> mapOfFields = buildDefendantDetails("false");
        final Plea plea = pleaBuilder.buildPlea(mapOfFields.build());
        assertThat(plea.getContactNumber(), is("1234"));
        assertThat(plea.getDrivingLicenceNumber(), is("ABCD"));
        assertThat(plea.getEmailAddress(), is("test@gmail.com"));
        assertThat(plea.getDetailsCorrect(), is(true));
    }

    @Test
    public void defendantPersonalDetailsChangesFalse() {
        PleaBuilder pleaBuilder = new PleaBuilder();
        final ImmutableMap.Builder<String, String> mapOfFields = buildDefendantDetails("true");
        final Plea plea = pleaBuilder.buildPlea(mapOfFields.build());
        assertThat(plea.getContactNumber(), is("1234"));
        assertThat(plea.getDrivingLicenceNumber(), is("ABCD"));
        assertThat(plea.getEmailAddress(), is("test@gmail.com"));
        assertThat(plea.getDetailsCorrect(), is(true));
    }

    @Test
    public void defendantPersonalDetailsAreWrong() {
        PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(of("contactNumber", "1234"
                , "drivingLicenceNumber", "ABCD"
                , "emailAddress", "test@gmail.com"
                , "detailsCorrectYes", "false"
                , "detailsCorrectNo", "false"));
        assertThat(plea.getContactNumber(), is("1234"));
        assertThat(plea.getDrivingLicenceNumber(), is("ABCD"));
        assertThat(plea.getEmailAddress(), is("test@gmail.com"));
        assertThat(plea.getDetailsCorrect(), is(false));
    }

    @Test
    public void defendantSelectedBothGuiltyAndNotGuiltyForMultipleOffences() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final String title1 = "Obstruct person executing search warrant for TV receiver";
        final String title2 = "Use / install a television set without a licence";
        final ImmutableMap.Builder<String, String> mapOfFields = buildOffencesWithPleas(title1, title2, "true", "true",
                "true", "true");
        Predicate<Offence> offenceTitle1Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title1);
        Predicate<Offence> offenceTitle2Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title2);
        final Plea plea = pleaBuilder.buildPlea(mapOfFields.build());
        assertThat(plea.getOffences().size(), is(2));
        final Offence offence1 = plea.getOffences().stream().filter(offenceTitle1Predicate).findFirst().get();
        assertThat(offence1.getPleaValue(), is(PleaValue.BOTH));
        final Offence offence2 = plea.getOffences().stream().filter(offenceTitle2Predicate).findFirst().get();
        assertThat(offence2.getPleaValue(), is(PleaValue.BOTH));
    }

    @Test
    public void defendantSelectedGuiltyAndNotGuiltyForMultipleOffences() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final String title1 = "Obstruct person executing search warrant for TV receiver";
        final String title2 = "Use / install a television set without a licence";
        final ImmutableMap.Builder<String, String> mapOfFields = buildOffencesWithPleas(title1, title2, "true", "false", "false", "true");
        Predicate<Offence> offenceTitle1Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title1);
        Predicate<Offence> offenceTitle2Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title2);
        final Plea plea = pleaBuilder.buildPlea(mapOfFields.build());
        assertThat(plea.getOffences().size(), is(2));
        final Offence offence1 = plea.getOffences().stream().filter(offenceTitle1Predicate).findFirst().get();
        assertThat(offence1.getPleaValue(), is(PleaValue.GUILTY));
        final Offence offence2 = plea.getOffences().stream().filter(offenceTitle2Predicate).findFirst().get();
        assertThat(offence2.getPleaValue(), is(PleaValue.NOT_GUILTY));
    }

    @Test
    public void defendantNotSelectedAnyPleaForMultipleOffences() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final String title1 = "Obstruct person executing search warrant for TV receiver";
        final String title2 = "Use / install a television set without a licence";
        final ImmutableMap.Builder<String, String> mapOfFields = buildOffencesWithPleas(title1, title2, "false",
                "false", "false", "false");
        Predicate<Offence> offenceTitle1Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title1);
        Predicate<Offence> offenceTitle2Predicate = (offence) -> offence.getTitle().equalsIgnoreCase(title2);
        final Plea plea = pleaBuilder.buildPlea(mapOfFields.build());
        assertThat(plea.getOffences().size(), is(2));
        final Offence offence1 = plea.getOffences().stream().filter(offenceTitle1Predicate).findFirst().get();
        assertThat(offence1.getPleaValue(), is(PleaValue.BOTH));
        final Offence offence2 = plea.getOffences().stream().filter(offenceTitle2Predicate).findFirst().get();
        assertThat(offence2.getPleaValue(), is(PleaValue.BOTH));
    }

    @Test
    public void defendantSelectedInterpreterLanguage() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildInterpreter("true", "false", "spanish").build());
        assertThat(plea.getInterpreter().getLanguage(), is("spanish"));
        assertThat(plea.getInterpreter().getNeeded(), is(true));
    }

    @Test
    public void defendantSelectedBothInterpreterOptions() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildInterpreter("true", "true", "spanish").build());
        assertThat(plea.getInterpreter().getLanguage(), is("spanish"));
        assertThat(plea.getInterpreter().getNeeded(), is(nullValue()));
    }

    @Test
    public void defendantWishesToComeToCourt() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildWishesToComeToCourt("true", "false").build());
        assertThat(plea.getWishToComeToCourt(), is(true));
    }

    @Test
    public void defendantSelectedBothWishesToComeToCourtAndNot() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildWishesToComeToCourt("true", "true").build());
        assertThat(plea.getWishToComeToCourt(), is(nullValue()));
    }

    @Test
    public void defendantSpeakWelshInWelshCourt() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildSpeakWelshInWelshCourt("true", "false").build());
        assertThat(plea.getWelshHearing(), is(true));
    }

    @Test
    public void defendantSelectedBothSpeakWelshInWelshCourtAndNot() {
        final PleaBuilder pleaBuilder = new PleaBuilder();
        final Plea plea = pleaBuilder.buildPlea(buildSpeakWelshInWelshCourt("true", "true").build());
        assertThat(plea.getWelshHearing(), is(nullValue()));
    }

    private ImmutableMap.Builder<String, String> buildWishesToComeToCourt(final String comeToCourtYes, final String comeToCourtNo){
        final ImmutableMap.Builder<String, String> mapOfFields = new ImmutableMap.Builder<String, String>();
        mapOfFields.put("wantToGoToCourtYes", comeToCourtYes);
        mapOfFields.put("wantToGoToCourtNo", comeToCourtNo);
        return mapOfFields;
    }

    private ImmutableMap.Builder<String, String> buildSpeakWelshInWelshCourt(final String comeToCourtYes, final String comeToCourtNo){
        final ImmutableMap.Builder<String, String> mapOfFields = new ImmutableMap.Builder<String, String>();
        mapOfFields.put("speakWelshInWelshCourt", comeToCourtYes);
        mapOfFields.put("speakEnglishInWelshCourt", comeToCourtNo);
        return mapOfFields;
    }

    private ImmutableMap.Builder<String, String> buildInterpreter(final String requiredYes, final String requiredNo, final String language) {
        final ImmutableMap.Builder<String, String> mapOfFields = new ImmutableMap.Builder<String, String>();
        mapOfFields.put("interpreterLanguage", language);
        mapOfFields.put("interpreterRequiredYes", requiredYes);
        mapOfFields.put("interpreterRequiredNo", requiredNo);
        return mapOfFields;
    }

    private ImmutableMap.Builder<String, String> buildOffencesWithPleas(final String title1,
                                                                        final String title2,
                                                                        final String offence1GuiltyPlea,
                                                                        final String offence1NotGuiltyPlea,
                                                                        final String offence2GuiltyPlea,
                                                                        final String offence2NotGuiltyPlea) {
        final ImmutableMap.Builder<String, String> mapOfFields = new ImmutableMap.Builder<String, String>();
        mapOfFields.put("pleaOffenceTitle1", title1);
        mapOfFields.put("pleaOffenceTitle2", title2);
        mapOfFields.put("pleaGuilty1", offence1GuiltyPlea);
        mapOfFields.put("pleaNotGuilty1", offence1NotGuiltyPlea);
        mapOfFields.put("pleaGuilty2", offence2GuiltyPlea);
        mapOfFields.put("pleaNotGuilty2", offence2NotGuiltyPlea);
        return mapOfFields;
    }


    private ImmutableMap.Builder<String, String> buildDefendantDetails(final String detailCorrectNo) {
        final ImmutableMap.Builder<String, String> mapOfFields = new ImmutableMap.Builder<String, String>();
        mapOfFields.put("contactNumber", "1234");
        mapOfFields.put("drivingLicenceNumber", "ABCD");
        mapOfFields.put("emailAddress", "test@gmail.com");
        mapOfFields.put("detailsCorrectYes", "true");
        mapOfFields.put("detailsCorrectNo", detailCorrectNo);
        mapOfFields.put("detailChanges", "false");
        return mapOfFields;
    }

}