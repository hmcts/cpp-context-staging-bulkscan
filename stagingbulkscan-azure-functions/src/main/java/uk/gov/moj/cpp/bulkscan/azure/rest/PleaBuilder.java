package uk.gov.moj.cpp.bulkscan.azure.rest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PleaBuilder {

    private static final int MAX_OFFENCE = 10;

    Plea buildPlea(final Map<String, String> metadataMap) {
        final Plea.Builder pleaBuilder = Plea.plea()
                .withContactNumber(metadataMap.get("contactNumber"))
                .withDrivingLicenceNumber(metadataMap.get("drivingLicenceNumber"))
                .withEmailAddress(metadataMap.get("emailAddress"));

        buildDetailsCorrect(metadataMap, pleaBuilder);
        buildWishToComeToCourt(metadataMap, pleaBuilder);
        buildWelshHearing(metadataMap, pleaBuilder);
        buildInterpreter(metadataMap, pleaBuilder);
        buildOffences(metadataMap, pleaBuilder);
        return pleaBuilder.build();
    }


    private void buildOffences(final Map<String, String> metadataMap, final Plea.Builder pleaBuilder) {
        final List<Offence> offenceList = new ArrayList<>();

        for (int index = 1; index <= MAX_OFFENCE; index++) {
            if (metadataMap.containsKey("pleaOffenceTitle" + index)) {
                final Offence offence = buildOffence(metadataMap, index);
                offenceList.add(offence);
            }
        }

        pleaBuilder.withOffences(offenceList);
    }

    private Offence buildOffence(final Map<String, String> metadataMap, final int index) {
        final Offence.Builder offenceBuilder = Offence.offence();
        final String offenceTitle = metadataMap.get("pleaOffenceTitle" + index);
        final boolean pleaGuilty = metadataMap.containsKey("pleaGuilty" + index) && Boolean.parseBoolean(metadataMap.get("pleaGuilty" + index));
        final boolean pleaNotGuilty = metadataMap.containsKey("pleaNotGuilty" + index) && Boolean.parseBoolean(metadataMap.get("pleaNotGuilty" + index));

        offenceBuilder.withTitle(offenceTitle);
        offenceBuilder.withPleaValue(PleaValue.NOT_GUILTY);

        if (pleaGuilty && !pleaNotGuilty) {
            offenceBuilder.withPleaValue(PleaValue.GUILTY);
        }

        if (pleaGuilty && pleaNotGuilty) {
            offenceBuilder.withPleaValue(PleaValue.BOTH);
        }

        if(!pleaGuilty && !pleaNotGuilty) {
            offenceBuilder.withPleaValue(PleaValue.BOTH);
        }

        return offenceBuilder.build();
    }

    private void buildInterpreter(final Map<String, String> metadataMap, final Plea.Builder pleaBuilder) {
        final Interpreter.Builder interpreterBuilder = Interpreter.interpreter();

        final String interpreterLanguage = "interpreterLanguage";

        if (metadataMap.containsKey(interpreterLanguage) && isNotBlank(metadataMap.get(interpreterLanguage))) {
            interpreterBuilder.withLanguage(metadataMap.get(interpreterLanguage));

            final boolean interpreterRequiredYes = metadataMap.containsKey("interpreterRequiredYes") && Boolean.parseBoolean(metadataMap.get("interpreterRequiredYes"));
            final boolean interpreterRequiredNo = metadataMap.containsKey("interpreterRequiredNo") && Boolean.parseBoolean(metadataMap.get("interpreterRequiredNo"));

            interpreterBuilder.withNeeded(false);

            if (interpreterRequiredYes && !interpreterRequiredNo) {
                interpreterBuilder.withNeeded(true);
            }

            if (interpreterRequiredYes && interpreterRequiredNo) {
                interpreterBuilder.withNeeded(null);
            }

            pleaBuilder.withInterpreter(interpreterBuilder.build());
        }
    }

    private void buildDetailsCorrect(final Map<String, String> metadataMap, final Plea.Builder pleaBuilder) {
        final boolean detailsCorrectYes = metadataMap.containsKey("detailsCorrectYes") && Boolean.parseBoolean(metadataMap.get("detailsCorrectYes"));
        final boolean detailsCorrectNo = metadataMap.containsKey("detailsCorrectNo") && Boolean.parseBoolean(metadataMap.get("detailsCorrectNo"));
        final boolean detailChanges = metadataMap.containsKey("detailChanges") && Boolean.parseBoolean(metadataMap.get("detailChanges"));

        pleaBuilder.withDetailsCorrect(false);

        if (detailsCorrectYes && !detailsCorrectNo && !detailChanges) {
            pleaBuilder.withDetailsCorrect(true);
        }

        if (detailsCorrectYes && detailsCorrectNo && !detailChanges) {
            pleaBuilder.withDetailsCorrect(true);
        }
    }

    private void buildWishToComeToCourt(final Map<String, String> metadataMap, final Plea.Builder pleaBuilder) {
        final boolean wantToGoToCourtYes = metadataMap.containsKey("wantToGoToCourtYes") && Boolean.parseBoolean(metadataMap.get("wantToGoToCourtYes"));
        final boolean wantToGoToCourtNo = metadataMap.containsKey("wantToGoToCourtNo") && Boolean.parseBoolean(metadataMap.get("wantToGoToCourtNo"));

        pleaBuilder.withWishToComeToCourt(false);

        if (wantToGoToCourtYes && !wantToGoToCourtNo) {
            pleaBuilder.withWishToComeToCourt(true);
        }

        if (wantToGoToCourtYes && wantToGoToCourtNo) {
            pleaBuilder.withWishToComeToCourt(null);
        }
    }

    private void buildWelshHearing(final Map<String, String> metadataMap, final Plea.Builder pleaBuilder) {
        final boolean speakWelshInWelshCourt = metadataMap.containsKey("speakWelshInWelshCourt") && Boolean.parseBoolean(metadataMap.get("speakWelshInWelshCourt"));
        final boolean speakEnglishInWelshCourt = metadataMap.containsKey("speakEnglishInWelshCourt") && Boolean.parseBoolean(metadataMap.get("speakEnglishInWelshCourt"));

        pleaBuilder.withWelshHearing(false);

        if (speakWelshInWelshCourt && !speakEnglishInWelshCourt) {
            pleaBuilder.withWelshHearing(true);
        }

        if (speakWelshInWelshCourt && speakEnglishInWelshCourt) {
            pleaBuilder.withWelshHearing(null);
        }
    }
}
