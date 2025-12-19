package uk.gov.moj.cpp.bulkscan.azure.function;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.bulkscan.azure.function.FunctionConstants.ZIP_FILE_NAME_DATE_FORMAT;

import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class FunctionConstantsTest {

    @Test
    void testUniqueCreateDocumentControlNumber () {
        final int docControlNoCount = 10010;

        final Set<String> result  = IntStream.range(0, docControlNoCount)
                .mapToObj(i -> FunctionConstants.createDocumentControlNumber())
                .collect(toSet());

        assertEquals(docControlNoCount, result.size());
    }

    @Test
    void testFormatTimestamp () {
        String result = FunctionConstants.formatTimestamp(ZIP_FILE_NAME_DATE_FORMAT);

        assertEquals(19, result.length());
    }
}
