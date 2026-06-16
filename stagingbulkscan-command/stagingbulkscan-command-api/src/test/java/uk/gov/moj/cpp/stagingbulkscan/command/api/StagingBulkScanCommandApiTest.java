package uk.gov.moj.cpp.stagingbulkscan.command.api;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingBulkScanCommandApiTest {

    private static final String PATH_TO_RAML = "src/raml/stagingbulkscan-command-api.raml";
    private static final List<String> NON_PASS_THROUGH_METHODS = newArrayList("registerScanEnvelope", "markAsAction");
    private static final String REGISTER_SCAN_ENVELOPE = "stagingbulkscan.command.register-scan-envelope";
    private static final String MARK_AS_ACTION_COMMAND = "stagingbulkscan.command.mark-as-action";
    private static final String NAME = "name:";
    private static final String KEY = "scanEnvelope";
    private static final String VALUE = "envelope";
    private static final String FIELD_SCAN_DOCUMENT_ID = "scanDocumentId";
    private static final String SCAN_DOCUMENT_ID = randomUUID().toString();

    private Map<String, String> apiMethodsToHandlerNames;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> senderArgumentCaptor;

    @InjectMocks
    private StagingBulkScanCommandApi stagingBulkScanCommandApi;

    @BeforeEach
    public void setup() {
        apiMethodsToHandlerNames = apiMethodsToHandlerNames();
    }

    @Test
    public void testActionNameAndHandlerNameAreSame() throws Exception {
        final List<String> allLines = FileUtils.readLines(new File(PATH_TO_RAML));

        final List<String> allHandlerNames = Optional.of(apiMethodsToHandlerNames.values().stream())
                .orElseGet(Stream::empty)
                .collect(toList());

        assertThat(allHandlerNames, containsInAnyOrder(allLines.stream()
                .filter(action -> !action.isEmpty())
                .filter(line -> line.contains(NAME))
                .map(line -> line.replaceAll(NAME, "").trim()).toArray()));
    }

    @Test
    public void testHandlerNamesPassThroughSender() {
        assertHandlerMethodsArePassThrough(apiMethodsToHandlerNames.keySet().stream()
                .filter(methodName -> !NON_PASS_THROUGH_METHODS.contains(methodName))
                .collect(toMap(identity(), apiMethodsToHandlerNames::get)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassRequestToCommandHandler() {
        final JsonObject requestPayload = createObjectBuilder().add(KEY, VALUE).build();
        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), requestPayload);
        stagingBulkScanCommandApi.registerScanEnvelope(commandJsonEnvelope);

        verify(sender).send(senderArgumentCaptor.capture());

        final Envelope<JsonObject> jsonEnvelopOut = (Envelope<JsonObject>) senderArgumentCaptor.getValue();
        final JsonObject jsonObject = jsonEnvelopOut.payload();
        assertThat(jsonEnvelopOut.metadata().name(), is(REGISTER_SCAN_ENVELOPE));
        assertThat(jsonObject.getString(KEY), is(VALUE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleMarkAsActionCommand() {

        final JsonObject requestPayload = createObjectBuilder().add(FIELD_SCAN_DOCUMENT_ID, SCAN_DOCUMENT_ID).build();
        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), requestPayload);
        stagingBulkScanCommandApi.markAsAction(commandJsonEnvelope);

        verify(sender).send(senderArgumentCaptor.capture());

        final Envelope<JsonObject> jsonEnvelopOut = (Envelope<JsonObject>) senderArgumentCaptor.getValue();
        final JsonObject jsonObject = jsonEnvelopOut.payload();
        assertThat(jsonEnvelopOut.metadata().name(), is(MARK_AS_ACTION_COMMAND));
        assertThat(jsonObject.getString(FIELD_SCAN_DOCUMENT_ID), is(SCAN_DOCUMENT_ID));
    }

    private void assertHandlerMethodsArePassThrough(final Map<String, String> methodsToHandlerNamesMap) {
        for (final Map.Entry<String, String> entry : methodsToHandlerNamesMap.entrySet()) {
            assertThat(StagingBulkScanCommandApi.class, isHandlerClass(COMMAND_API)
                    .with(method(entry.getKey())
                            .thatHandles(entry.getValue())
                            .withSenderPassThrough()));
        }
    }

    private Map<String, String> apiMethodsToHandlerNames() {
        return stream(StagingBulkScanCommandApi.class.getMethods())
                .filter(method -> method.getAnnotation(Handles.class) != null)
                .collect(toMap(Method::getName, method -> method.getAnnotation(Handles.class).value()));
    }
}