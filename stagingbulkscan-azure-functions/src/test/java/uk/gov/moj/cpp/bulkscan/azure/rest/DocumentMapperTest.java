package uk.gov.moj.cpp.bulkscan.azure.rest;


import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

public class DocumentMapperTest {

    @Test
    public void shouldGetMappedValueFor_DisqualificationNoticeResponse() {
        assertThat(DocumentMapper.getDocumentMapper().get("Disqualification Notice Response"), CoreMatchers.is("DISQUALIFICATION_REPLY_SLIP"));
    }
}