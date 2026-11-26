package uk.gov.moj.cpp.stagingbulkscan.utils;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.JsonObject;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String QUEUE_URI = "tcp://" + HOST + ":61616";

    private static final long RETRIEVE_TIMEOUT = 20000;

    private static Session session;

    private Queue queue;
    private Topic topic;
    private String topicName;

    public static final QueueUtil privateCommandQueue = new QueueUtil("jms.queue.stagingbulkscan.handler.command", false);

    private QueueUtil(final String name, final boolean isTopic) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            final Connection connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            if (isTopic) {
                topic = new ActiveMQTopic(name);
                this.topicName = name;
            } else {
                queue = new ActiveMQQueue(name);
            }
            this.topicName = name;
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }


    public MessageProducer createProducer() {
        try {
            return session.createProducer(queue);
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }


    public static void sendMessage(final MessageProducer messageProducer, final String commandName, final JsonObject payload, final Metadata metadata) {
        
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);

        final String json = jsonEnvelope.asJsonObject().toString();

        try {
            final TextMessage message = session.createTextMessage();

            message.setText(json);
            message.setStringProperty("CPPNAME", commandName);

            messageProducer.send(message);
        } catch (final JMSException e) {
            throw new RuntimeException("Failed to send message. commandName: '" + commandName + "', json: " + json, e);
        }
    }

}
