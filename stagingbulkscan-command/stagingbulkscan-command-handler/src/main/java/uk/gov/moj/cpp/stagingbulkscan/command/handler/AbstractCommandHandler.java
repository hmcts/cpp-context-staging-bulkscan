package uk.gov.moj.cpp.stagingbulkscan.command.handler;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

abstract class AbstractCommandHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    <A extends Aggregate> A aggregate(final Class<A> clazz, final UUID streamId,
                                      final Envelope<?> envelope, final Function<A, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(streamId);
        final A aggregate = aggregateService.get(eventStream, clazz);
        final Stream<Object> events = function.apply(aggregate);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
        return aggregate;
    }

    UUID getUserId(final JsonEnvelope envelope) {
        return envelope.metadata().userId().map(UUID::fromString)
                .orElse(null);
    }

    <T> UUID getUserId(final Envelope<T> envelope) {
        return envelope.metadata().userId().map(UUID::fromString).orElse(null);
    }
}