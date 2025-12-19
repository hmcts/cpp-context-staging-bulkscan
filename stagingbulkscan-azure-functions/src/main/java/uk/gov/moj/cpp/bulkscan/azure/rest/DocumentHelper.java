package uk.gov.moj.cpp.bulkscan.azure.rest;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static uk.gov.moj.cpp.bulkscan.azure.rest.DocumentMapper.getDocumentMapper;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

public class DocumentHelper {

    private final ReferenceDataQueryHelper referenceDataQueryHelper;

    public DocumentHelper(final ReferenceDataQueryHelper referenceDataQueryHelper) {
        this.referenceDataQueryHelper = referenceDataQueryHelper;
    }

    public String determineDocumentStatus(final String caseUrn, final String casePTIUrn, final String prosecutorId, final String documentName) {

        final Predicate<DocumentMapper> predicate = documentMapper -> documentMapper.isDocumentSupported(documentName);

        final BiPredicate<String, String> caseUrnPredicate = StringUtils::isNoneBlank;

        final boolean isDocumentSupport = predicate.test(getDocumentMapper());

        if (isDocumentSupport) {
            if (caseUrnPredicate.test(caseUrn, prosecutorId)) {
                return "PENDING";
            }

            if (isNoneBlank(casePTIUrn)) {
                final String ouCode = referenceDataQueryHelper.getOuCodeByPtiUrn(casePTIUrn).getString("oucode");
                if (isNoneBlank(ouCode)) {
                    return "PENDING";
                }
            }
        }

        return "FOLLOW_UP";
    }
}
