package uk.gov.moj.cpp.bulkscan.azure.function.helper;

import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.ENGLISH_COVER_FOOTER;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_ENG;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.MULT_WEL;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.SJP_MC100;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.SJP_NOTI;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.SJP_PLEA;
import static uk.gov.moj.cpp.bulkscan.azure.function.helper.TemplateValidationHelper.WELSH_COVER_FOOTER;

import uk.gov.moj.cpp.bulkscan.azure.function.util.PageContent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class TemplateValidationData {

    public static List<PageContent> createValidMultilingualPageContents() {
        List<PageContent> welshPages = createValidWelshPageContents(1);
        List<PageContent> englishPages = createValidEnglishPageContents(welshPages.get(welshPages.size() - 1).getPageNumber() + 1);

        List<PageContent> combined = new java.util.ArrayList<>(welshPages);
        combined.addAll(englishPages);

        return combined;
    }

    public static List<PageContent> createValidEnglishPageContents(final int startPageNumber) {
        final List<PageContent> pages = new ArrayList<>();
        pages.addAll(createValidEnglishCoverPageContents(startPageNumber));
        pages.addAll(createValidEnglishSJPNPageContents(pages.get(pages.size() - 1).getPageNumber() + 1));
        pages.addAll(createValidEnglishPleaContents(pages.get(pages.size() - 1).getPageNumber() + 1));
        pages.addAll(createValidEnglishMC100Contents(pages.get(pages.size() - 1).getPageNumber() + 1));
        return pages;
    }

    public static List<PageContent> createValidWelshPageContents(final int startPageNumber) {
        final List<PageContent> pages = new ArrayList<>();
        pages.addAll(createValidWelshCoverPageContents(startPageNumber));
        pages.addAll(createValidWelshSJPNPageContents(pages.get(pages.size() - 1).getPageNumber() + 1));
        pages.addAll(createValidWelshPleaContents(pages.get(pages.size() - 1).getPageNumber() + 1));
        pages.addAll(createValidWelshMC100Contents(pages.get(pages.size() - 1).getPageNumber() + 1));
        return pages;
    }

    public static byte[] createValidMultilingualPdfDocument() throws IOException {
        PDDocument document = new PDDocument();
        try {
            List<PageContent> pages = createValidMultilingualPageContents();
            for (PageContent page : pages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        } finally {
            document.close();
        }
    }

    public static byte[] createValidEnglishPdfDocument() throws IOException {
        PDDocument document = new PDDocument();
        try {
            List<PageContent> pages = createValidEnglishPageContents(1);
            for (PageContent page : pages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        } finally {
            document.close();
        }
    }

    public static byte[] createInvalidPdfDocument() throws IOException {
        PDDocument document = new PDDocument();
        try {
            addPageWithText(document, "This is the first page of an invalid PDF document.");
            List<PageContent> englishPages = createValidEnglishPageContents(1);
            for (PageContent page : englishPages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        } finally {
            document.close();
        }
    }

    public static byte[] createEmptyPdfDocument() throws IOException {
        PDDocument document = new PDDocument();
        try {
            return documentToByteArray(document);
        } finally {
            document.close();
        }
    }

    public static void addPageWithText(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        try {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 12);

            float startX = 50;
            float startY = 750;
            float leading = 14.5f;

            contentStream.setLeading(leading);
            contentStream.newLineAtOffset(startX, startY);

            String[] lines = text.split("\n");
            for (String line : lines) {
                String cleanLine = line.replaceAll("[\r\t]", " ");
                contentStream.showText(cleanLine);
                contentStream.newLine();
            }
            contentStream.endText();
        } finally {
            contentStream.close();
        }
    }

    public static byte[] documentToByteArray(PDDocument document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }

    public static PDDocument createTestPDDocument(int pageCount, final List<String> pageContents) throws IOException {
        PDDocument document = new PDDocument();
        for (int i = 0; i < pageCount; i++) {
            addPageWithText(document, pageContents.get(i));
        }
        return document;
    }

    public static List<PageContent> createValidEnglishCoverPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "Mr Samuel Brown\n" +
                        "Flat 5\n" +
                        "12 Somerandom Road\n" +
                        "London\n" +
                        "CR0 1AB\n" +
                        "Posting Date: 09/07/2025\n" +
                        "URN/Case no: 874133\n" +
                        "Dear Mr Samuel Mcclusky ,\n" +
                        "We’ve sent you a Single Justice Procedure notice\n" +
                        "because you have been charged with the offence on the\n" +
                        "Charge Sheet overleaf.\n" +
                        "GALMT00." + ENGLISH_COVER_FOOTER),
                new PageContent(startPageNumber + 1, "URN/Case no: ABC123 Some Random text for Cover page 2 \n" +
                        "GALMT00." + ENGLISH_COVER_FOOTER)
        );
    }

    public static List<PageContent> createValidEnglishSJPNPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "URN/Case no: ABC123\n" +
                        "Mr Samuel Brown\n" +
                        "Flat 5\n" +
                        "12 Somerandom Road\n" +
                        "London\n" +
                        "CR0 1AB\n" +
                        "Date of birth: 24/10/1992 Posting Date: 09/07/2025\n" +
                        "Single Justice Procedure Notice\n" +
                        "You have been charged with: the offence on the Charge Sheet\n" +
                        "What you need to do\n" +
                        "Make your plea telling us whether you are guilty or not guilty by30/07/2025.\n" +
                        "Your case will then be reviewed by the court and you’ll get a letter within 28 days.\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_ENG),
                new PageContent(startPageNumber + 1, "URN/Case no: ABC123\n" +
                        "For help to make your plea or with the court process call 0300 303 0656 (English)\n" +
                        "or 0300 303 5172 (if the offence occurred in Wales and you wish to speak Welsh).\n" +
                        "Please note they cannot give you legal advice.\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_ENG),
                new PageContent(startPageNumber + 2, "URN/Case no: ABC123\n" +
                        "Charge Sheet\n" +
                        "You have been charged with the following criminal offence(s)\n" +
                        "Offence 1 Enter a Greater Manchester Metrolink vehicle / station without a valid ticket.\n" +
                        "The Greater Manchester Metrolink system byelaws\n" +
                        "Transport for Greater Manchester, pursuant to the powers conferred upon it by the\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_ENG),
                new PageContent(startPageNumber + 3, "Statement of facts\n" +
                        "* If you plead not guilty but are found guilty by the court the amount of prosecution costs you may\n" +
                        "be ordered to pay could be much higher.\n" +
                        "Statement of facts\n" +
                        "Statement of CSR T476\n" +
                        "CHD: 09 August 2025\n" +
                        "This statement (consisting of one page(s) each signed by me) is true to the best of my\n" +
                        "knowledge and belief and I make it knowing that, if it is tendered in evidence, I shall be liable to\n" +
                        "prosecution if I wilfully stated in it anything which I know to be false or do not believe to be\n" +
                        "true.\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_ENG),
                new PageContent(startPageNumber + 4, "Single Justice Procedure explained\n" +
                        "Why have we sent you a Single Justice Procedure notice?\n" +
                        "You have been charged with a motoring offence(s). The Single Justice Procedure notice " +
                        "contains the details of the charge against you.\n" +
                        "What is the Single Justice Procedure?\n" +
                        "A single justice procedure case is dealt with in the same way as any other case, except that"),
                new PageContent(startPageNumber + 5, "What happens in court\n" +
                        "If you have told us that you disagree with the statement(s) on your notice, in section 4 for " +
                        "guilty pleas or section 5 for not guilty pleas, you’ll have the chance at the hearing to " +
                        "question or challenge the statement(s) made.\n" +
                        "You can represent yourself in court – you do not need legal support if you do not want it.")
        );
    }

    public static List<PageContent> createValidEnglishPleaContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "URN/Case no: ABC123\n" +
                        "Make your plea by post\n" +
                        "Fill in this paper form to plead by post - or you can plead online" +
                        "1. Your details\n" +
                        "Name Mr Samuel Brown\n" +
                        "Address Flat 5, 12 Somerandom Road, London, CR0 1AB\n" +
                        "Date of birth 24/10/1992" +
                        "2. Additional details\n" +
                        "The court may need to contact you, so they can deal with your case more quickly\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(startPageNumber + 1, "URN/Case no: ABC123\n" +
                        "3. Your plea\n" +
                        "Pleading guilty: This means you agree you committed the offence. You must say if you\n" +
                        "want to come to court or not.\n" +
                        "Pleading not guilty: This means you do not agree you committed the offence. You must\n" +
                        "come to court. You’ll get a letter with the court date. You must explain why you think you\n" +
                        "are not guilty.\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(startPageNumber + 2, "URN/Case no: ABC123\n" +
                        "4. Guilty plea\n" +
                        "Fill in this page if you are pleading guilty\n" +
                        "4.1 If you plead Guilty to all offences you must tell us if you want to come to court\n" +
                        "You do not need to attend court but have the right to. The court can consider your case\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(startPageNumber + 3, "URN/Case no: ABC123\n" +
                        "5. Not guilty plea\n" +
                        "Fill in this page if you are pleading not guilty\n" +
                        "5.1 Why do you believe you are not guilty?\n" +
                        "You need to explain why you think you’re not guilty. Tell us if you disagree with the\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(startPageNumber + 4, "URN/Case no: ABC123\n" +
                        "6. Your court hearing\n" +
                        "Fill in this page if you are pleading not guilty or guilty and requesting a court hearing.\n" +
                        "You will be sent a letter to let you know the court date and address. There will be instructions on\n" +
                        "the form telling you what to do if you would like the case heard in another court.\n" +
                        "6.1 Are there any dates you cannot attend court?\n" +
                        "The court will try to avoid these when setting a date for the hearing. If you do not provide\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG),
                new PageContent(startPageNumber + 5, "URN/Case no: ABC123\n" +
                        "7. Your declaration\n" +
                        "! You may be prosecuted if you provide false information or deliberately do\n" +
                        "not declare all the relevant facts\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_ENG)
        );
    }

    public static List<PageContent> createValidEnglishMC100Contents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "URN/Case no: ABC123\n" +
                        "Your finances\n" +
                        "Name Mr Samuel Brown\n" +
                        "Address Flat 5, 12 Somerandom Road, London, CR0 1AB\n" +
                        "Date of birth 24/10/1992\n" +
                        "Please provide details of your finances\n" +
                        "If you are found guilty and need to pay a penalty, then the court will decide the amount\n" +
                        "based on your finances and the seriousness of the offence.\n" +
                        "8. Your income\n" +
                        "8.1 Your average take home income (i.e. after tax)\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(startPageNumber + 1, "URN/Case no: ABC123\n" +
                        "9. Deductions from your earnings or benefits\n" +
                        "If you are found guilty and need to pay a penalty you can choose to have it deducted from\n" +
                        "your earnings or benefits.\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(startPageNumber + 2, "URN/Case no: ABC123\n" +
                        "9. Deductions from your earnings or benefits (continued)\n" +
                        "9.6 If yes, which benefits?\n" +
                        "10. Your monthly outgoings and assets\n" +
                        "This information will help the court to understand your financial situation and work out the\n" +
                        "best way for you to be able to pay any financial penalty\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(startPageNumber + 3, "URN/Case no: ABC123\n" +
                        "11. Company finances * Only fill in if you're pleading on behalf of a company\n" +
                        "11.1 Has the company been trading for 12 months?\n" +
                        "Post your completed form to:\n" +
                        "HMCTS Crime\n" +
                        "PO Box 12888\n" +
                        "HARLOW\n" +
                        "CM20 9RW\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_ENG),
                new PageContent(startPageNumber + 4, "Please insert all evidence, statements, " +
                        "photos etc and add additional pages as required"),
                new PageContent(startPageNumber + 5, "This page is for the Certificate of Service " +
                        "that must be at the end of the complete pack, once you have inserted all evidence etc")
        );
    }

    public static List<PageContent> createValidWelshCoverPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "Mr Samuel Brown\n" +
                        "Flat 5\n" +
                        "12 Somerandom Road\n" +
                        "London\n" +
                        "CR0 1AB\n" +
                        "Dyddiad Postio: 09/07/2025\n" +
                        "Cyfeirnod Unigryw/Rhif yr Achos:  874133\n" +
                        "Annwyl Samuel Brown,\n" +
                        "Rydym wedi anfon Hysbysiad Gweithdrefn Un Ynad atoch oherwydd rydych wedi cael eich cyhuddo " +
                        "o gyflawni\n" +
                        "GALMT00." + WELSH_COVER_FOOTER),
                new PageContent(startPageNumber + 1, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123 Cadwch " +
                        "y llythyr hwn er mwyn ichi allu cyfeirio \n" +
                        "GALMT00." + WELSH_COVER_FOOTER)
        );
    }

    public static List<PageContent> createValidWelshSJPNPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Mr Samuel Brown\n" +
                        "Flat 5\n" +
                        "12 Somerandom Road\n" +
                        "London\n" +
                        "CR0 1AB\n" +
                        "Dyddiad geni: 24/10/1992 Dyddiad postio: 09/07/2025\n" +
                        "Hysbysiad Gweithdrefn Un Ynad\n" +
                        "Rydych wedi cael eich cyhuddo o gyflawni:\n" +
                        "Beth sydd angen i chi ei wneud\n" +
                        "Dweud wrthym p’un a ydych yn euog ynteu’n ddieuog drwy gofnodi eich ple erbyn\n" +
                        "Yna bydd y llys yn adolygu eich achos a byddwch yn cael llythyr o fewn 28 diwrnod.\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_WEL),
                new PageContent(startPageNumber + 1, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "For help to make your plea or with the court process call 0300 303 0656 (English)\n" +
                        "or 0300 303 5172 (if the offence occurred in Wales and you wish to speak Welsh).\n" +
                        "Please note they cannot give you legal advice.\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_WEL),
                new PageContent(startPageNumber + 2, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Taflen Gyhuddiadau\n" +
                        "Rydych wedi cael eich cyhuddo o gyflawni’r trosedd(au) canlynol:\n" +
                        "Cosb Ariannol\n" +
                        "Nid oes angen i chi dalu unrhyw beth nawr.\n" +
                        "Os bydd y llys yn penderfynu eich bod yn euog, efallai y bydd rhaid ichi dalu cosb ariannol, " +
                        "sydd fel arfer yn cynnwys pedair rhan:\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_WEL),
                new PageContent(startPageNumber + 3, "Datganiad ffeithiau\n" +
                        "GALMT00." + SJP_NOTI + "." + MULT_WEL),
                new PageContent(startPageNumber + 4, "Mwy o wybodaeth am y Weithdrefn Un Ynad\n" +
                        "Pam ein bod wedi anfon Hysbysiad Gweithdrefn Un Ynad atoch?\n" +
                        "Rydych wedi cael eich cyhuddo o gyflawni trosedd(au) moduro. Mae’r Hysbysiad Gweithdrefn Un Ynad " +
                        "yn cynnwys manylion y cyhuddiad(au) yn eich erbyn. \n"),
                new PageContent(startPageNumber + 5, "Beth fydd yn digwydd yn y llys\n" +
                        "Os ydych wedi dweud wrthym eich bod yn anghytuno â’r datganiad(au) ar eich hysbysiad, yn adran " +
                        "4 ar gyfer pleon euog neu adran 5 ar gyfer pleon dieuog, bydd gennych gyfle yn y gwrandawiad i " +
                        "gwestiynu neu herio’r datganiad(au) a wnaed. \n")
        );
    }

    public static List<PageContent> createValidWelshPleaContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Cofnodi eich ple drwy’r post\n" +
                        "Dylech lenwi’r ffurflen bapur hon i gofnodi ple drwy’r post - neu gallwch gofnodi ple ar-lein\n" +
                        "1. Eich manylion Enw\n" +
                        "2. Manylion Ychwanegol\n" +
                        "Efallai bydd y llys angen cysylltu â chi fel y gallant ddelio â’ch achos yn gyflymach\n" +
                        "2.1 Rhif ffôn cyswllt\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(startPageNumber + 1, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "3. Eich ple\n" +
                        "Pledio’n euog: Golyga hyn eich bod yn cytuno eich bod wedi cyflawni’r trosedd. Mae’n rhaid ichi " +
                        "ddweud os ydych eisiau dod i’r llys ai peidio.\n" +
                        "Pledio’n ddieuog: Golyga hyn nad ydych yn cytuno eich bod wedi cyflawni’r trosedd. Mae’n rhaid " +
                        "ichi ddod i’r llys. Byddwch yn cael llythyr gyda dyddiad llys. Mae’n rhaid ichi egluro pam ydych " +
                        "yn credu eich bod yn ddieuog.\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(startPageNumber + 2, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "4. Pledio’n euog\n" +
                        "Dylech lenwi’r dudalen hon os ydych yn pledio’n euog \n" +
                        "4.1 Os ydych yn pledio’n Euog i bob trosedd, mae’n rhaid ichi ddweud wrthym os ydych eisiau dod i’r llys\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(startPageNumber + 3, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "5. Pledio’n ddieuog\n" +
                        "Dylech lenwi’r dudalen hon os ydych yn pledio’n ddieuog\n" +
                        "5.1 Pam ydych chi’n credu eich bod yn ddieuog?\n" +
                        "Mae angen ichi egluro pam ydych chi’n credu eich bod yn ddieuog. Dywedwch wrthym os ydych yn " +
                        "anghytuno â’r datganiad(au) yn yr hysbysiad hwn a pham. Os na fyddwch yn darparu’r wybodaeth hon:\n" +
                        "5.2 Ydych chi eisiau dod â’ch tystion eich hun? (os yw’n berthnasol)\n" +
                        "Gallai fod yn rhywun i roi tystiolaeth yn y llys i gefnogi eich ple dieuog\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(startPageNumber + 4, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "6. Eich gwrandawiad llys\n" +
                        "Dylech lenwi’r dudalen hon os ydych yn pledio’n ddieuog neu’n euog ac yn gwneud cais am wrandawiad llys.\n" +
                        "6.1 A oes yna unrhyw ddyddiadau pan na allwch fynychu’r llys?\n" +
                        "Bydd y llys yn ceisio osgoi’r rhain pan fydd yn pennu dyddiad ar gyfer y gwrandawiad. \n" +
                        "6.2 Anghenion ieithyddol\n" +
                        "6.2.1 Ydych chi angen cyfieithydd yn y llys?\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL),
                new PageContent(startPageNumber + 5, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "7. Eich datganiad\n" +
                        "!\tGellir eich erlyn os byddwch yn darparu gwybodaeth anwir neu’n peidio â datgelu’r holl " +
                        "ffeithiau perthnasol yn fwriadol\n" +
                        "GALMT00." + SJP_PLEA + "." + MULT_WEL)
        );
    }

    public static List<PageContent> createValidWelshMC100Contents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "Eich amgylchiadau ariannol\n" +
                        "Enw Mr Samuel Brown\n" +
                        "Cyfeiriad Flat 5, 12 Somerandom Road, London, CR0 1AB\n" +
                        "Dyddiad geni 24/10/1992\n" +
                        "Nodwch fanylion llawn eich amgylchiadau ariannol\n" +
                        "Os dyfernir eich bod yn euog a bod rhaid ichi dalu dirwy, bydd y llys yn penderfynu ar y swm " +
                        "yn seiliedig ar eich sefyllfa ariannol a difrifoldeb y drosedd.\n" +
                        "8. Eich incwm\n" +
                        "8.1 Eich incwm ar gyfartaledd (h.y. ar ôl tynnu treth)\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(startPageNumber + 1, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "9. Didyniadau o’ch enillion neu’ch budd-daliadau\n" +
                        "Os penderfynir eich bod yn euog a bod rhaid ichi dalu cosb ariannol, gallwch ddewis i’r gosb " +
                        "gael ei didynnu o’ch enillion neu’ch budd-daliadau.\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(startPageNumber + 2, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "9. Didyniadau o’ch enillion neu’ch budd-daliadau (parhad)\n" +
                        "9.6 Os ‘ydw’, pa fudd-daliadau?\n" +
                        "10. Eich holl wariant misol a’ch asedau\n" +
                        "Bydd yr wybodaeth hon yn helpu’r llys i ddeall eich sefyllfa ariannol a chyfrifo’r ffordd orau " +
                        "ichi allu talu unrhyw gosb ariannol\n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(startPageNumber + 3, "Cyfeirnod Unigryw/Rhif yr Achos: ABC123\n" +
                        "11. Sefyllfa ariannol y cwmni * Dim ond os ydych yn pledio ar ran cwmni y dylech lenwi’r adran hon\n" +
                        "11.1 A yw’r cwmni wedi bod yn masnachu am 12 mis?\n" +
                        "Postiwch eich ffurflen wedi’i llenwi i:\n" +
                        "HMCTS Crime\n" +
                        "PO Box 12888\n" +
                        "HARLOW\n" +
                        "CM20 9RW \n" +
                        "GALMT00." + SJP_MC100 + "." + MULT_WEL),
                new PageContent(startPageNumber + 4, "Rhowch yr holl dystiolaeth, datganiadau, " +
                        "ffotograffau ac ati ac ychwanegwch dudalennau ychwanegol yn ôl yr angen"),
                new PageContent(startPageNumber + 5, " \n" +
                        "Mae’r dudalen hon ar gyfer y Dystysgrif Gwasanaeth y mae’n rhaid iddi fod ar " +
                        "ddiwedd y pecyn cyflawn, unwaith y byddwch wedi mewnosod yr holl dystiolaeth ac at")
        );
    }
}
