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
        try (PDDocument document = new PDDocument()) {
            List<PageContent> pages = createValidMultilingualPageContents();
            for (PageContent page : pages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        }
    }

    public static byte[] createValidEnglishPdfDocument() throws IOException {
        try (PDDocument document = new PDDocument()) {
            List<PageContent> pages = createValidEnglishPageContents(1);
            for (PageContent page : pages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        }
    }

    public static byte[] createInvalidPdfDocument() throws IOException {
        try (PDDocument document = new PDDocument()) {
            addPageWithText(document, "This is the first page of an invalid PDF document.");
            List<PageContent> englishPages = createValidEnglishPageContents(1);
            for (PageContent page : englishPages) {
                addPageWithText(document, page.getText());
            }
            return documentToByteArray(document);
        }
    }

    public static byte[] createEmptyPdfDocument() throws IOException {
        try (PDDocument document = new PDDocument()) {
            return documentToByteArray(document);
        }
    }

    public static void addPageWithText(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
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
                new PageContent(startPageNumber, """
                        Mr Samuel Brown
                        Flat 5
                        12 Somerandom Road
                        London
                        CR0 1AB
                        Posting Date: 09/07/2025
                        URN/Case no: 874133
                        Dear Mr Samuel Mcclusky ,
                        We’ve sent you a Single Justice Procedure notice
                        because you have been charged with the offence on the
                        Charge Sheet overleaf.
                        GALMT00.%s""".formatted(ENGLISH_COVER_FOOTER)),
                new PageContent(startPageNumber + 1, """
                        URN/Case no: ABC123 Some Random text for Cover page 2
                        GALMT00.%s""".formatted(ENGLISH_COVER_FOOTER))
        );
    }

    public static List<PageContent> createValidEnglishSJPNPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        URN/Case no: ABC123
                        Mr Samuel Brown
                        Flat 5
                        12 Somerandom Road
                        London
                        CR0 1AB
                        Date of birth: 24/10/1992 Posting Date: 09/07/2025
                        Single Justice Procedure Notice
                        You have been charged with: the offence on the Charge Sheet
                        What you need to do
                        Make your plea telling us whether you are guilty or not guilty by30/07/2025.
                        Your case will then be reviewed by the court and you’ll get a letter within 28 days.
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_ENG)),
                new PageContent(startPageNumber + 1, """
                        URN/Case no: ABC123
                        For help to make your plea or with the court process call 0300 303 0656 (English)
                        or 0300 303 5172 (if the offence occurred in Wales and you wish to speak Welsh).
                        Please note they cannot give you legal advice.
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_ENG)),
                new PageContent(startPageNumber + 2, """
                        URN/Case no: ABC123
                        Charge Sheet
                        You have been charged with the following criminal offence(s)
                        Offence 1 Enter a Greater Manchester Metrolink vehicle / station without a valid ticket.
                        The Greater Manchester Metrolink system byelaws
                        Transport for Greater Manchester, pursuant to the powers conferred upon it by the
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_ENG)),
                new PageContent(startPageNumber + 3, """
                        Statement of facts
                        * If you plead not guilty but are found guilty by the court the amount of prosecution costs you may
                        be ordered to pay could be much higher.
                        Statement of facts
                        Statement of CSR T476
                        CHD: 09 August 2025
                        This statement (consisting of one page(s) each signed by me) is true to the best of my
                        knowledge and belief and I make it knowing that, if it is tendered in evidence, I shall be liable to
                        prosecution if I wilfully stated in it anything which I know to be false or do not believe to be
                        true.
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_ENG)),
                new PageContent(startPageNumber + 4, """
                        Single Justice Procedure explained
                        Why have we sent you a Single Justice Procedure notice?
                        You have been charged with a motoring offence(s). The Single Justice Procedure notice contains the details of the charge against you.
                        What is the Single Justice Procedure?
                        A single justice procedure case is dealt with in the same way as any other case, except that"""),
                new PageContent(startPageNumber + 5, """
                        What happens in court
                        If you have told us that you disagree with the statement(s) on your notice, in section 4 for guilty pleas or section 5 for not guilty pleas, you’ll have the chance at the hearing to question or challenge the statement(s) made.
                        You can represent yourself in court – you do not need legal support if you do not want it.""")
        );
    }

    public static List<PageContent> createValidEnglishPleaContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        URN/Case no: ABC123
                        Make your plea by post
                        Fill in this paper form to plead by post - or you can plead online
                        1. Your details
                        Name Mr Samuel Brown
                        Address Flat 5, 12 Somerandom Road, London, CR0 1AB
                        Date of birth 24/10/1992
                        2. Additional details
                        The court may need to contact you, so they can deal with your case more quickly
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG)),
                new PageContent(startPageNumber + 1, """
                        URN/Case no: ABC123
                        3. Your plea
                        Pleading guilty: This means you agree you committed the offence. You must say if you
                        want to come to court or not.
                        Pleading not guilty: This means you do not agree you committed the offence. You must
                        come to court. You’ll get a letter with the court date. You must explain why you think you
                        are not guilty.
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG)),
                new PageContent(startPageNumber + 2, """
                        URN/Case no: ABC123
                        4. Guilty plea
                        Fill in this page if you are pleading guilty
                        4.1 If you plead Guilty to all offences you must tell us if you want to come to court
                        You do not need to attend court but have the right to. The court can consider your case
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG)),
                new PageContent(startPageNumber + 3, """
                        URN/Case no: ABC123
                        5. Not guilty plea
                        Fill in this page if you are pleading not guilty
                        5.1 Why do you believe you are not guilty?
                        You need to explain why you think you’re not guilty. Tell us if you disagree with the
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG)),
                new PageContent(startPageNumber + 4, """
                        URN/Case no: ABC123
                        6. Your court hearing
                        Fill in this page if you are pleading not guilty or guilty and requesting a court hearing.
                        You will be sent a letter to let you know the court date and address. There will be instructions on
                        the form telling you what to do if you would like the case heard in another court.
                        6.1 Are there any dates you cannot attend court?
                        The court will try to avoid these when setting a date for the hearing. If you do not provide
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG)),
                new PageContent(startPageNumber + 5, """
                        URN/Case no: ABC123
                        7. Your declaration
                        ! You may be prosecuted if you provide false information or deliberately do
                        not declare all the relevant facts
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_ENG))
        );
    }

    public static List<PageContent> createValidEnglishMC100Contents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        URN/Case no: ABC123
                        Your finances
                        Name Mr Samuel Brown
                        Address Flat 5, 12 Somerandom Road, London, CR0 1AB
                        Date of birth 24/10/1992
                        Please provide details of your finances
                        If you are found guilty and need to pay a penalty, then the court will decide the amount
                        based on your finances and the seriousness of the offence.
                        8. Your income
                        8.1 Your average take home income (i.e. after tax)
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_ENG)),
                new PageContent(startPageNumber + 1, """
                        URN/Case no: ABC123
                        9. Deductions from your earnings or benefits
                        If you are found guilty and need to pay a penalty you can choose to have it deducted from
                        your earnings or benefits.
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_ENG)),
                new PageContent(startPageNumber + 2, """
                        URN/Case no: ABC123
                        9. Deductions from your earnings or benefits (continued)
                        9.6 If yes, which benefits?
                        10. Your monthly outgoings and assets
                        This information will help the court to understand your financial situation and work out the
                        best way for you to be able to pay any financial penalty
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_ENG)),
                new PageContent(startPageNumber + 3, """
                        URN/Case no: ABC123
                        11. Company finances * Only fill in if you're pleading on behalf of a company
                        11.1 Has the company been trading for 12 months?
                        Post your completed form to:
                        HMCTS Crime
                        PO Box 12888
                        HARLOW
                        CM20 9RW
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_ENG)),
                new PageContent(startPageNumber + 4, """
                        Please insert all evidence, statements, photos etc and add additional pages as required"""),
                new PageContent(startPageNumber + 5, """
                        This page is for the Certificate of Service that must be at the end of the complete pack, once you have inserted all evidence etc""")
        );
    }

    public static List<PageContent> createValidWelshCoverPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        Mr Samuel Brown
                        Flat 5
                        12 Somerandom Road
                        London
                        CR0 1AB
                        Dyddiad Postio: 09/07/2025
                        Cyfeirnod Unigryw/Rhif yr Achos:  874133
                        Annwyl Samuel Brown,
                        Rydym wedi anfon Hysbysiad Gweithdrefn Un Ynad atoch oherwydd rydych wedi cael eich cyhuddo o gyflawni
                        GALMT00.%s""".formatted(WELSH_COVER_FOOTER)),
                new PageContent(startPageNumber + 1, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123 Cadwch y llythyr hwn er mwyn ichi allu cyfeirio
                        GALMT00.%s""".formatted(WELSH_COVER_FOOTER))
        );
    }

    public static List<PageContent> createValidWelshSJPNPageContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        Mr Samuel Brown
                        Flat 5
                        12 Somerandom Road
                        London
                        CR0 1AB
                        Dyddiad geni: 24/10/1992 Dyddiad postio: 09/07/2025
                        Hysbysiad Gweithdrefn Un Ynad
                        Rydych wedi cael eich cyhuddo o gyflawni:
                        Beth sydd angen i chi ei wneud
                        Dweud wrthym p’un a ydych yn euog ynteu’n ddieuog drwy gofnodi eich ple erbyn
                        Yna bydd y llys yn adolygu eich achos a byddwch yn cael llythyr o fewn 28 diwrnod.
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_WEL)),
                new PageContent(startPageNumber + 1, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        For help to make your plea or with the court process call 0300 303 0656 (English)
                        or 0300 303 5172 (if the offence occurred in Wales and you wish to speak Welsh).
                        Please note they cannot give you legal advice.
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_WEL)),
                new PageContent(startPageNumber + 2, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        Taflen Gyhuddiadau
                        Rydych wedi cael eich cyhuddo o gyflawni’r trosedd(au) canlynol:
                        Cosb Ariannol
                        Nid oes angen i chi dalu unrhyw beth nawr.
                        Os bydd y llys yn penderfynu eich bod yn euog, efallai y bydd rhaid ichi dalu cosb ariannol, sydd fel arfer yn cynnwys pedair rhan:
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_WEL)),
                new PageContent(startPageNumber + 3, """
                        Datganiad ffeithiau
                        GALMT00.%s.%s""".formatted(SJP_NOTI, MULT_WEL)),
                new PageContent(startPageNumber + 4, """
                        Mwy o wybodaeth am y Weithdrefn Un Ynad
                        Pam ein bod wedi anfon Hysbysiad Gweithdrefn Un Ynad atoch?
                        Rydych wedi cael eich cyhuddo o gyflawni trosedd(au) moduro. Mae’r Hysbysiad Gweithdrefn Un Ynad yn cynnwys manylion y cyhuddiad(au) yn eich erbyn.
                        """),
                new PageContent(startPageNumber + 5, """
                        Beth fydd yn digwydd yn y llys
                        Os ydych wedi dweud wrthym eich bod yn anghytuno â’r datganiad(au) ar eich hysbysiad, yn adran 4 ar gyfer pleon euog neu adran 5 ar gyfer pleon dieuog, bydd gennych gyfle yn y gwrandawiad i gwestiynu neu herio’r datganiad(au) a wnaed.
                        """)
        );
    }

    public static List<PageContent> createValidWelshPleaContents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        Cofnodi eich ple drwy’r post
                        Dylech lenwi’r ffurflen bapur hon i gofnodi ple drwy’r post - neu gallwch gofnodi ple ar-lein
                        1. Eich manylion Enw
                        2. Manylion Ychwanegol
                        Efallai bydd y llys angen cysylltu â chi fel y gallant ddelio â’ch achos yn gyflymach
                        2.1 Rhif ffôn cyswllt
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL)),
                new PageContent(startPageNumber + 1, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        3. Eich ple
                        Pledio’n euog: Golyga hyn eich bod yn cytuno eich bod wedi cyflawni’r trosedd. Mae’n rhaid ichi ddweud os ydych eisiau dod i’r llys ai peidio.
                        Pledio’n ddieuog: Golyga hyn nad ydych yn cytuno eich bod wedi cyflawni’r trosedd. Mae’n rhaid ichi ddod i’r llys. Byddwch yn cael llythyr gyda dyddiad llys. Mae’n rhaid ichi egluro pam ydych yn credu eich bod yn ddieuog.
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL)),
                new PageContent(startPageNumber + 2, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        4. Pledio’n euog
                        Dylech lenwi’r dudalen hon os ydych yn pledio’n euog
                        4.1 Os ydych yn pledio’n Euog i bob trosedd, mae’n rhaid ichi ddweud wrthym os ydych eisiau dod i’r llys
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL)),
                new PageContent(startPageNumber + 3, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        5. Pledio’n ddieuog
                        Dylech lenwi’r dudalen hon os ydych yn pledio’n ddieuog
                        5.1 Pam ydych chi’n credu eich bod yn ddieuog?
                        Mae angen ichi egluro pam ydych chi’n credu eich bod yn ddieuog. Dywedwch wrthym os ydych yn anghytuno â’r datganiad(au) yn yr hysbysiad hwn a pham. Os na fyddwch yn darparu’r wybodaeth hon:
                        5.2 Ydych chi eisiau dod â’ch tystion eich hun? (os yw’n berthnasol)
                        Gallai fod yn rhywun i roi tystiolaeth yn y llys i gefnogi eich ple dieuog
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL)),
                new PageContent(startPageNumber + 4, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        6. Eich gwrandawiad llys
                        Dylech lenwi’r dudalen hon os ydych yn pledio’n ddieuog neu’n euog ac yn gwneud cais am wrandawiad llys.
                        6.1 A oes yna unrhyw ddyddiadau pan na allwch fynychu’r llys?
                        Bydd y llys yn ceisio osgoi’r rhain pan fydd yn pennu dyddiad ar gyfer y gwrandawiad.
                        6.2 Anghenion ieithyddol
                        6.2.1 Ydych chi angen cyfieithydd yn y llys?
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL)),
                new PageContent(startPageNumber + 5, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        7. Eich datganiad
                        !\tGellir eich erlyn os byddwch yn darparu gwybodaeth anwir neu’n peidio â datgelu’r holl ffeithiau perthnasol yn fwriadol
                        GALMT00.%s.%s""".formatted(SJP_PLEA, MULT_WEL))
        );
    }

    public static List<PageContent> createValidWelshMC100Contents(final int startPageNumber) {
        return Arrays.asList(
                new PageContent(startPageNumber, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        Eich amgylchiadau ariannol
                        Enw Mr Samuel Brown
                        Cyfeiriad Flat 5, 12 Somerandom Road, London, CR0 1AB
                        Dyddiad geni 24/10/1992
                        Nodwch fanylion llawn eich amgylchiadau ariannol
                        Os dyfernir eich bod yn euog a bod rhaid ichi dalu dirwy, bydd y llys yn penderfynu ar y swm yn seiliedig ar eich sefyllfa ariannol a difrifoldeb y drosedd.
                        8. Eich incwm
                        8.1 Eich incwm ar gyfartaledd (h.y. ar ôl tynnu treth)
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_WEL)),
                new PageContent(startPageNumber + 1, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        9. Didyniadau o’ch enillion neu’ch budd-daliadau
                        Os penderfynir eich bod yn euog a bod rhaid ichi dalu cosb ariannol, gallwch ddewis i’r gosb gael ei didynnu o’ch enillion neu’ch budd-daliadau.
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_WEL)),
                new PageContent(startPageNumber + 2, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        9. Didyniadau o’ch enillion neu’ch budd-daliadau (parhad)
                        9.6 Os ‘ydw’, pa fudd-daliadau?
                        10. Eich holl wariant misol a’ch asedau
                        Bydd yr wybodaeth hon yn helpu’r llys i ddeall eich sefyllfa ariannol a chyfrifo’r ffordd orau ichi allu talu unrhyw gosb ariannol
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_WEL)),
                new PageContent(startPageNumber + 3, """
                        Cyfeirnod Unigryw/Rhif yr Achos: ABC123
                        11. Sefyllfa ariannol y cwmni * Dim ond os ydych yn pledio ar ran cwmni y dylech lenwi’r adran hon
                        11.1 A yw’r cwmni wedi bod yn masnachu am 12 mis?
                        Postiwch eich ffurflen wedi’i llenwi i:
                        HMCTS Crime
                        PO Box 12888
                        HARLOW
                        CM20 9RW
                        GALMT00.%s.%s""".formatted(SJP_MC100, MULT_WEL)),
                new PageContent(startPageNumber + 4, """
                        Rhowch yr holl dystiolaeth, datganiadau, ffotograffau ac ati ac ychwanegwch dudalennau ychwanegol yn ôl yr angen"""),
                new PageContent(startPageNumber + 5, """
                        Mae’r dudalen hon ar gyfer y Dystysgrif Gwasanaeth y mae’n rhaid iddi fod ar diwedd y pecyn cyflawn, unwaith y byddwch wedi mewnosod yr holl dystiolaeth ac at""")
        );
    }
}
