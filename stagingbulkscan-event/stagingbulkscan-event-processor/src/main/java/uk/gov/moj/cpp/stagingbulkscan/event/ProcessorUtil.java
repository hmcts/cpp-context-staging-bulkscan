package uk.gov.moj.cpp.stagingbulkscan.event;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ProcessorUtil {

    private ProcessorUtil() {
    }

    private static String ukGovPostCodeRegex = "^(([gG][iI][rR] {0,}0[aA]{2})|(([aA][sS][cC][nN]|[sS][tT][hH][lL]|[tT][dD][cC][uU]|[bB][bB][nN][dD]|[bB][iI][qQ][qQ]|[fF][iI][qQ][qQ]|[pP][cC][rR][nN]|[sS][iI][qQ][qQ]|[iT][kK][cC][aA]) {0,}1[zZ]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yxA-HK-XY]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) [0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$";

    /**
     * UK Gov post code validation following CJS Data Standards
     *
     * @param postcode
     * @return Boolean
     */
    public static Boolean isUkGovPostCodeValidWithSpace(final String postcode) {
        if (isNotBlank(postcode)) {
            Pattern pattern = Pattern.compile(ukGovPostCodeRegex);
            Matcher matcher = pattern.matcher(postcode);
            return matcher.matches();
        }
        return Boolean.TRUE;
    }

    /**
     * Fix postcode by adding space
     *
     * @param postcode
     * @return fixed postcode with space  or Empty string if it cannot be fixed
     */
    public static String fixPostCodeSpacing(final String postcode) {
        if (!isUkGovPostCodeValidWithSpace(postcode)) {
            final String SINGLE_SPACE_CHAR = " ";
            String correctPostcode = postcode.replace(SINGLE_SPACE_CHAR, StringUtils.EMPTY);
            int index = correctPostcode.length() - 3;
            String fixedPostcode = correctPostcode.substring(0, index) +
                    SINGLE_SPACE_CHAR + correctPostcode.substring(index, correctPostcode.length());
            if (isUkGovPostCodeValidWithSpace(fixedPostcode)) {
                return fixedPostcode;
            }

        }
        return StringUtils.EMPTY;
    }
}
