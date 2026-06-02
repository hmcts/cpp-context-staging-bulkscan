package uk.gov.moj.cpp.stagingbulkscan.query.api;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.io.FileUtils.readLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingbulkscan.query.view.StagingBulkScanQueryView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StagingBulkScanQueryApiTest {

    private static final String PATH_TO_RAML = "src/raml/stagingbulkscan-query-api.raml";
    private static final String NAME = "name:";

    private Map<String, String> apiMethodsToHandlerNames;

    @BeforeEach
    public void setup() {
        apiMethodsToHandlerNames = stream(StagingBulkScanQueryApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }

    @Test
    public void shouldDelegateGetDocumentsForDeletionToQueryView() {
        final StagingBulkScanQueryView queryView = mock(StagingBulkScanQueryView.class);
        final StagingBulkScanQueryApi api = new StagingBulkScanQueryApi();
        setField(api, "stagingBulkScanQueryView", queryView);

        final JsonEnvelope query = mock(JsonEnvelope.class);
        final JsonEnvelope expected = mock(JsonEnvelope.class);
        when(queryView.getDocumentsForDeletion(query)).thenReturn(expected);

        final JsonEnvelope result = api.getDocumentsForDeletion(query);

        assertThat(result, org.hamcrest.Matchers.is(expected));
        verify(queryView).getDocumentsForDeletion(query);
    }

    @Test
    public void testActionNameAndHandleNameAreSame() throws Exception {
        final List<String> ramlActionNames = readLines(new File(PATH_TO_RAML)).stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(NAME))
                .map(line -> line.replaceAll(NAME, "").trim())
                .collect(toList());

        assertThat(apiMethodsToHandlerNames.values(), containsInAnyOrder(ramlActionNames.toArray()));
    }
}
