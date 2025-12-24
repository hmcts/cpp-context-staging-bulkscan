package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;
import uk.gov.moj.cpp.bulkscan.azure.function.util.ValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@SuppressWarnings("java:S115")
public class TemplateValidationHelper {

    public static final int COVER_SECTION_PAGES_TO_VALIDATE = 2;
    public static final int SJPN_SECTION_PAGES_TO_VALIDATE = 1;
    public static final int PLEA_SECTION_PAGES_TO_VALIDATE = 6;
    public static final int MC100_SECTION_PAGES_TO_VALIDATE = 4;

    public static final String SJP_LETT = "SJP.LETT";
    public static final String SJP_NOTI = "SJP.NOTI";
    public static final String SJP_PLEA = "SJP.PLEA";
    public static final String SJP_MC100 = "SJP.MC100";
    public static final String ENG = "ENG";
    public static final String MULT_ENG = "MULT.ENG";
    public static final String MULT_WEL = "MULT.WEL";
    public static final String ENGLISH_COVER_FOOTER = "SJP.LETT.MULT.ENG";
    public static final String WELSH_COVER_FOOTER = "SJP.LETT.MULT.WEL";

    public static final String URN_CASE_NO_ENG = "URN/Case no";
    public static final String POSTING_DATE_ENG = "Posting Date";
    public static final String SINGLE_JUSTICE_PROCEDURE_NOTICE_ENG = "Single Justice Procedure Notice";
    public static final String MAKE_YOUR_PLEA_BY_POST_ENG = "Make your plea by post";
    public static final String YOUR_DETAILS_ENG = "1. Your details";
    public static final String ADDITIONAL_DETAILS_ENG = "2. Additional details";
    public static final String YOUR_PLEA_ENG = "3. Your plea";
    public static final String GUILTY_PLEA_ENG = "4. Guilty plea";
    public static final String NOT_GUILTY_PLEA_ENG = "5. Not guilty plea";
    public static final String YOUR_COURT_HEARING_ENG = "6. Your court hearing";
    public static final String YOUR_DECLARATION_ENG = "7. Your declaration";
    public static final String YOUR_FINANCES_ENG = "Your finances";
    public static final String YOUR_INCOME_ENG = "8. Your income";
    public static final String DEDUCTIONS_FROM_EARNINGS_ENG = "9. Deductions from your earnings or benefits";
    public static final String MONTHLY_OUTGOINGS_ENG = "10. Your monthly outgoings and assets";
    public static final String COMPANY_FINANCES_ENG = "11. Company finances";
    public static final String POST_COMPLETED_FORM_ENG = "Post your completed form to";

    public static final String URN_CASE_NO_WEL = "Cyfeirnod Unigryw/Rhif yr Achos";
    public static final String POSTING_DATE_WEL = "Dyddiad Postio";
    public static final String SINGLE_JUSTICE_PROCEDURE_NOTICE_WEL = "Hysbysiad Gweithdrefn Un Ynad";
    public static final String MAKE_YOUR_PLEA_BY_POST_WEL = "Cofnodi eich ple drwy’r post";
    public static final String YOUR_DETAILS_WEL = "1. Eich manylion";
    public static final String ADDITIONAL_DETAILS_WEL = "2. Manylion Ychwanegol";
    public static final String YOUR_PLEA_WEL = "3. Eich ple";
    public static final String GUILTY_PLEA_WEL = "4. Pledio’n euog";
    public static final String NOT_GUILTY_PLEA_WEL = "5. Pledio’n ddieuog";
    public static final String YOUR_COURT_HEARING_WEL = "6. Eich gwrandawiad llys";
    public static final String YOUR_DECLARATION_WEL = "7. Eich datganiad";
    public static final String YOUR_FINANCES_WEL = "Eich amgylchiadau ariannol";
    public static final String YOUR_INCOME_WEL = "8. Eich incwm";
    public static final String DEDUCTIONS_FROM_EARNINGS_WEL = "9. Didyniadau o’ch enillion neu’ch budd-daliadau";
    public static final String MONTHLY_OUTGOINGS_WEL = "10. Eich holl wariant misol a’ch asedau";
    public static final String COMPANY_FINANCES_WEL = "11. Sefyllfa ariannol y cwmni";
    public static final String POST_COMPLETED_FORM_WEL = "Postiwch eich ffurflen wedi’i llenwi i";

    public enum DocumentType {
        ENGLISH,
        MULTILINGUAL,
        INVALID
    }

    public enum Language {
        English,
        Welsh
    }

    public static ValidationResult validateDocument(final byte[] documentContent) throws IOException {
        final ValidationResult result = new ValidationResult();

        try (final PDDocument document = PDDocument.load(documentContent)) {
            final int totalPages = document.getNumberOfPages();
            result.addDetail("Total pages in document: " + totalPages);

            final List<PageContent> pageContents = extractPageContents(document);

            if (pageContents.size() < 15) {
                result.addError("Invalid document: Document does not have enough pages, expected at least 15 pages");
                result.setDocumentType(DocumentType.INVALID.name());
                return result;
            } else if (pageContents.get(0).getText().isEmpty()) {
                result.addError("Invalid document: Document first page empty");
                result.setDocumentType(DocumentType.INVALID.name());
                return result;
            }

            final DocumentType documentType = determineDocumentType(pageContents.get(0));
            result.setDocumentType(documentType.name());

            if (DocumentType.INVALID.equals(documentType)) {
                result.addError("Invalid document: First page footer must contain either '" + ENGLISH_COVER_FOOTER + "' for English or '" + WELSH_COVER_FOOTER + "' for Multilingual (Welsh and English)");
                return result;
            }

            if (pageContents.isEmpty()) {
                result.addError("Invalid document: No pages found in document");
                return result;
            } else {
                result.addDetail("Document type detected: " + documentType.name());
                validateDocument(documentType, pageContents, result);
            }
        }

        return result;
    }

    private static void validateDocument(final DocumentType documentType, final List<PageContent> pageContents, final ValidationResult result) {
        if (result.getDocumentType() == null) {
            result.setDocumentType(determineDocumentType(pageContents.get(0)).name());
        }

        if (DocumentType.ENGLISH.equals(documentType)) {
            validateSections(Language.English, pageContents, result);
        } else if (DocumentType.MULTILINGUAL.equals(documentType)) {
            final int englishSectionStartIndex = findEnglishSectionStartIndex(pageContents);
            if (englishSectionStartIndex > 0) {
                validateSections(Language.Welsh, pageContents.subList(0, englishSectionStartIndex), result);
                validateSections(Language.English, pageContents.subList(englishSectionStartIndex, pageContents.size()), result);
            } else {
                result.addError("Could not find English section in Multilingual (Welsh and English) document which is expected to start with '" + ENGLISH_COVER_FOOTER + "' in footer");
            }
        }
    }

    private static void validateSections(final Language language, final List<PageContent> pageContents, final ValidationResult result) {
        validateCoverSection(language, pageContents, result);
        validateSJPNSection(language, pageContents, result);
        validatePleaSection(language, pageContents, result);
        validateMC100Section(language, pageContents, result);
    }

    private static void validateCoverSection(final Language language, final List<PageContent> pageContents, final ValidationResult result) {
        final String logText = language.name().concat(" Cover Section: ");

        final int coverStartIndex = 0;

        validatePageOnOddSequence(result, pageContents.get(0).getPageNumber(), logText);

        for (int pageNo = 1; pageNo <= COVER_SECTION_PAGES_TO_VALIDATE; pageNo++) {
            checkExpectedTexts(pageContents, result, logText,
                    coverStartIndex, pageNo, getExpectedTextsForCoverSection(language, pageNo));
        }
    }

    private static void validateSJPNSection(final Language language, final List<PageContent> pageContents, final ValidationResult result) {
        final String logText = language.name().concat(" SJPN Section: ");

        final int sjpnStartIndex = findSJPNStartIndex(language, pageContents);
        if (sjpnStartIndex == -1) {
            result.addError(logText + "Missing SJPN start page");
            return;
        }

        validatePageOnOddSequence(result, pageContents.get(sjpnStartIndex).getPageNumber(), logText);

        for (int pageNo = 1; pageNo <= SJPN_SECTION_PAGES_TO_VALIDATE; pageNo++) {
            checkExpectedTexts(pageContents, result, logText,
                    sjpnStartIndex, pageNo, getExpectedTextsForSJPNSection(language, pageNo));
        }
    }

    private static void validatePleaSection(final Language language, final List<PageContent> pageContents, final ValidationResult result) {
        final String logText = language.name().concat(" Plea Section: ");

        final int pleaStartIndex = findPleaStartIndex(language, pageContents);
        if (pleaStartIndex == -1) {
            result.addError(logText + "Missing Plea Form start page");
            return;
        }

        validatePageOnOddSequence(result, pageContents.get(pleaStartIndex).getPageNumber(), logText);

        for (int pageNo = 1; pageNo <= PLEA_SECTION_PAGES_TO_VALIDATE; pageNo++) {
            checkExpectedTexts(pageContents, result, logText,
                    pleaStartIndex, pageNo, getExpectedTextsForPleaSection(language, pageNo));
        }
    }

    private static void validateMC100Section(final Language language, final List<PageContent> pageContents, final ValidationResult result) {
        final String logText = language.name().concat(" MC100 Section: ");

        final int mc100StartIndex = findMC100StartIndex(language, pageContents);
        if (mc100StartIndex == -1) {
            result.addError(logText + "Missing MC100 Form start page");
            return;
        }

        validatePageOnOddSequence(result, pageContents.get(mc100StartIndex).getPageNumber(), logText);

        for (int pageNo = 1; pageNo <= MC100_SECTION_PAGES_TO_VALIDATE; pageNo++) {
            checkExpectedTexts(pageContents, result, logText,
                    mc100StartIndex, pageNo, getExpectedTextsForMC100Section(language, pageNo));
        }
    }

    private static DocumentType determineDocumentType(final PageContent firstPage) {
        final String pageText = firstPage.getText();

        if (contains(pageText, ENGLISH_COVER_FOOTER)) {
            return DocumentType.ENGLISH;
        } else if (contains(pageText, WELSH_COVER_FOOTER)) {
            return DocumentType.MULTILINGUAL;
        } else {
            return DocumentType.INVALID;
        }
    }

    private static int findEnglishSectionStartIndex(final List<PageContent> pageContents) {
        for (int i = 0; i < pageContents.size(); i++) {
            if (contains(pageContents.get(i).getText(), ENGLISH_COVER_FOOTER)) {
                return i;
            }
        }
        return -1;
    }

    private static int findSJPNStartIndex(final Language language, final List<PageContent> pageContents) {
        for (int i = 0; i < pageContents.size(); i++) {
            final PageContent page = pageContents.get(i);
            final String pageText = page.getText();

            if ((Language.English.equals(language)
                    && (contains(pageText, SJP_NOTI.concat(".").concat(MULT_ENG)) || contains(pageText, SJP_NOTI.concat(".").concat(ENG))))
                    || (Language.Welsh.equals(language) && contains(pageText, SJP_NOTI.concat(".").concat(MULT_WEL)))) {
                return i;
            }
        }
        return -1;
    }

    private static int findPleaStartIndex(final Language language, final List<PageContent> pageContents) {
        for (int i = 0; i < pageContents.size(); i++) {
            final PageContent page = pageContents.get(i);
            final String pageText = page.getText();

            if ((Language.English.equals(language)
                    && (contains(pageText, SJP_PLEA.concat(".").concat(MULT_ENG)) || contains(pageText, SJP_PLEA.concat(".").concat(ENG))))
                    || (Language.Welsh.equals(language) && contains(pageText, SJP_PLEA.concat(".").concat(MULT_WEL)))) {
                return i;
            }
        }
        return -1;
    }

    private static int findMC100StartIndex(final Language language, final List<PageContent> pageContents) {
        for (int i = 0; i < pageContents.size(); i++) {
            final PageContent page = pageContents.get(i);
            final String pageText = page.getText();

            if ((Language.English.equals(language)
                    && (contains(pageText, SJP_MC100.concat(".").concat(MULT_ENG)) || contains(pageText, SJP_MC100.concat(".").concat(ENG))))
                    || (Language.Welsh.equals(language) && contains(pageText, SJP_MC100.concat(".").concat(MULT_WEL)))) {
                return i;
            }
        }
        return -1;
    }

    private static void validatePageOnOddSequence(final ValidationResult result, final int pageNumber, final String logText) {
        if (pageNumber % 2 == 0) {
            result.addError(logText + "First page is not on odd sequence - Page " + pageNumber);
        } else {
            result.addDetail(logText + "First page is on odd sequence - Page " + pageNumber);
        }
    }

    private static void checkExpectedTexts(final List<PageContent> pageContents, final ValidationResult result, final String logText,
                                           final int startIndex, final int pageNo, final String[] expectedTexts) {
        for (final String expectedText : expectedTexts) {
            if (!contains(pageContents.get(startIndex + pageNo - 1).getText(), expectedText)) {
                result.addError(logText + "page " + pageNo + " - '" + expectedText + "' missing");
            } else {
                result.addDetail(logText + "page " + pageNo + " - '" + expectedText + "' found");
            }
        }
    }

    private static String[] getExpectedTextsForCoverSection(final Language language, final int pageNumber) {
        String[] expectedTexts = new String[0];
        if (Language.English.equals(language)) {
            switch (pageNumber) {
                case 1:
                    expectedTexts = new String[]{URN_CASE_NO_ENG, POSTING_DATE_ENG, SJP_LETT};
                    break;
                case 2:
                    expectedTexts = new String[]{SJP_LETT};
                    break;
                default:
                    break;
            }
        } else if (Language.Welsh.equals(language)) {
            switch (pageNumber) {
                case 1:
                    expectedTexts = new String[]{URN_CASE_NO_WEL, POSTING_DATE_WEL, SJP_LETT};
                    break;
                case 2:
                    expectedTexts = new String[]{SJP_LETT};
                    break;
                default:
                    break;
            }
        }
        return expectedTexts;
    }

    private static String[] getExpectedTextsForSJPNSection(final Language language, final int pageNumber) {
        String[] expectedTexts = new String[0];

        if (Language.English.equals(language)) {
            switch (pageNumber) {
                case 1:
                    expectedTexts = new String[]{URN_CASE_NO_ENG, SINGLE_JUSTICE_PROCEDURE_NOTICE_ENG, POSTING_DATE_ENG, SJP_NOTI};
                    break;
                default:
                    break;
            }
        } else if (Language.Welsh.equals(language)) {
            switch (pageNumber) {
                case 1:
                    expectedTexts = new String[]{URN_CASE_NO_WEL, SINGLE_JUSTICE_PROCEDURE_NOTICE_WEL, POSTING_DATE_WEL, SJP_NOTI};
                    break;
                default:
                    break;
            }
        }
        return expectedTexts;
    }

    private static String[] getExpectedTextsForPleaSection(final Language language, final int pageNumber) {
        if (Language.English.equals(language)) {
            return getExpectedTextsForPleaSectionEnglish(pageNumber);
        } else if (Language.Welsh.equals(language)) {
            return getExpectedTextsForPleaSectionWelsh(pageNumber);
        } else {
            return new String[0];
        }
    }

    private static String[] getExpectedTextsForPleaSectionEnglish(final int pageNumber) {
        String[] expectedTexts = new String[0];
        switch (pageNumber) {
            case 1:
                expectedTexts = new String[]{URN_CASE_NO_ENG, MAKE_YOUR_PLEA_BY_POST_ENG, YOUR_DETAILS_ENG, ADDITIONAL_DETAILS_ENG, SJP_PLEA};
                break;
            case 2:
                expectedTexts = new String[]{URN_CASE_NO_ENG, YOUR_PLEA_ENG, SJP_PLEA};
                break;
            case 3:
                expectedTexts = new String[]{URN_CASE_NO_ENG, GUILTY_PLEA_ENG, SJP_PLEA};
                break;
            case 4:
                expectedTexts = new String[]{URN_CASE_NO_ENG, NOT_GUILTY_PLEA_ENG, SJP_PLEA};
                break;
            case 5:
                expectedTexts = new String[]{URN_CASE_NO_ENG, YOUR_COURT_HEARING_ENG, SJP_PLEA};
                break;
            case 6:
                expectedTexts = new String[]{URN_CASE_NO_ENG, YOUR_DECLARATION_ENG, SJP_PLEA};
                break;
            default:
                break;
        }
        return expectedTexts;
    }

    private static String[] getExpectedTextsForPleaSectionWelsh(final int pageNumber) {
        String[] expectedTexts = new String[0];
        switch (pageNumber) {
            case 1:
                expectedTexts = new String[]{URN_CASE_NO_WEL, MAKE_YOUR_PLEA_BY_POST_WEL, YOUR_DETAILS_WEL, ADDITIONAL_DETAILS_WEL, SJP_PLEA};
                break;
            case 2:
                expectedTexts = new String[]{URN_CASE_NO_WEL, YOUR_PLEA_WEL, SJP_PLEA};
                break;
            case 3:
                expectedTexts = new String[]{URN_CASE_NO_WEL, GUILTY_PLEA_WEL, SJP_PLEA};
                break;
            case 4:
                expectedTexts = new String[]{URN_CASE_NO_WEL, NOT_GUILTY_PLEA_WEL, SJP_PLEA};
                break;
            case 5:
                expectedTexts = new String[]{URN_CASE_NO_WEL, YOUR_COURT_HEARING_WEL, SJP_PLEA};
                break;
            case 6:
                expectedTexts = new String[]{URN_CASE_NO_WEL, YOUR_DECLARATION_WEL, SJP_PLEA};
                break;
            default:
                break;
        }
        return expectedTexts;
    }

    private static String[] getExpectedTextsForMC100Section(final Language language, final int pageNumber) {
        if (Language.English.equals(language)) {
            return getExpectedTextsForMC100SectionEnglish(pageNumber);
        } else if (Language.Welsh.equals(language)) {
            return getExpectedTextsForMC100SectionWelsh(pageNumber);
        } else {
            return new String[0];
        }
    }

    private static String[] getExpectedTextsForMC100SectionEnglish(final int pageNumber) {
        String[] expectedTexts = new String[0];
        switch (pageNumber) {
            case 1:
                expectedTexts = new String[]{URN_CASE_NO_ENG, YOUR_FINANCES_ENG, YOUR_INCOME_ENG, SJP_MC100};
                break;
            case 2:
                expectedTexts = new String[]{URN_CASE_NO_ENG, DEDUCTIONS_FROM_EARNINGS_ENG, SJP_MC100};
                break;
            case 3:
                expectedTexts = new String[]{URN_CASE_NO_ENG, DEDUCTIONS_FROM_EARNINGS_ENG, MONTHLY_OUTGOINGS_ENG, SJP_MC100};
                break;
            case 4:
                expectedTexts = new String[]{URN_CASE_NO_ENG, COMPANY_FINANCES_ENG, POST_COMPLETED_FORM_ENG, SJP_MC100};
                break;
            default:
                break;
        }
        return expectedTexts;
    }

    private static String[] getExpectedTextsForMC100SectionWelsh(final int pageNumber) {
        String[] expectedTexts = new String[0];
        switch (pageNumber) {
            case 1:
                expectedTexts = new String[]{URN_CASE_NO_WEL, YOUR_FINANCES_WEL, YOUR_INCOME_WEL, SJP_MC100};
                break;
            case 2:
                expectedTexts = new String[]{URN_CASE_NO_WEL, DEDUCTIONS_FROM_EARNINGS_WEL, SJP_MC100};
                break;
            case 3:
                expectedTexts = new String[]{URN_CASE_NO_WEL, DEDUCTIONS_FROM_EARNINGS_WEL, MONTHLY_OUTGOINGS_WEL, SJP_MC100};
                break;
            case 4:
                expectedTexts = new String[]{URN_CASE_NO_WEL, COMPANY_FINANCES_WEL, POST_COMPLETED_FORM_WEL, SJP_MC100};
                break;
            default:
                break;
        }
        return expectedTexts;
    }

    private static List<PageContent> extractPageContents(final PDDocument document) throws IOException {
        List<PageContent> pageContents = new ArrayList<>();
        PDFTextStripper textStripper = new PDFTextStripper();

        for (int i = 1; i <= document.getNumberOfPages(); i++) {
            textStripper.setStartPage(i);
            textStripper.setEndPage(i);
            String pageText = textStripper.getText(document);
            pageContents.add(new PageContent(i, pageText));
        }

        return pageContents;
    }

    private static boolean contains(final String mainText, final String searchText) {
        if (mainText == null || searchText == null) {
            return false;
        }

        final String normalizedMainText = mainText.replaceAll("[\u0027\u2019\u2018\u201B\u2032\\s]", "").trim().toUpperCase();
        final String normalizedSearchText = searchText.replaceAll("[\u0027\u2019\u2018\u201B\u2032\\s]", "").trim().toUpperCase();

        return normalizedMainText.contains(normalizedSearchText);
    }
}