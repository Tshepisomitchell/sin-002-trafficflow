package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class CongestionPublisher implements AutoCloseable {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;

    public CongestionPublisher() throws JMSException {
        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(
                        MqConfig.BROKER_URL
                );

        connection = factory.createConnection();
        connection.start();

        session = connection.createSession(
                false,
                Session.AUTO_ACKNOWLEDGE
        );

        Topic topic = session.createTopic(
                MqConfig.TOPIC
        );

        producer = session.createProducer(topic);
    }

    public void publish(CongestionEvent event)
            throws Exception {

        String json = mapper.writeValueAsString(event);

        TextMessage message =
                session.createTextMessage(json);

        producer.send(message);

        System.out.printf(
                "Published congestion event: level=%d%n",
                event.level()
        );
    }

    @Override
    public void close() {
        try {
            producer.close();
        } catch (JMSException ignored) {
        }

        try {
            session.close();
        } catch (JMSException ignored) {
        }

        try {
            connection.close();
        } catch (JMSException ignored) {
        }
    }
}