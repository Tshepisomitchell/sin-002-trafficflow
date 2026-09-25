package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.concurrent.atomic.AtomicReference;

public class CongestionSubscriber implements AutoCloseable {

    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicReference<Integer> latestLevel =
            new AtomicReference<>();

    private final Connection connection;
    private final Session session;
    private final MessageConsumer consumer;

    public CongestionSubscriber() throws JMSException {
        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(
                        MqConfig.BROKER_URL
                );

        connection = factory.createConnection();

        session = connection.createSession(
                false,
                Session.AUTO_ACKNOWLEDGE
        );

        Topic topic = session.createTopic(
                MqConfig.TOPIC
        );

        consumer = session.createConsumer(topic);

        consumer.setMessageListener(message -> {
            if (!(message instanceof TextMessage textMessage)) {
                return;
            }

            try {
                CongestionEvent event = mapper.readValue(
                        textMessage.getText(),
                        CongestionEvent.class
                );

                latestLevel.set(event.level());

                System.out.printf(
                        "Received congestion event: level=%d%n",
                        event.level()
                );

            } catch (Exception exception) {
                System.err.println(
                        "Could not process congestion event: "
                                + exception.getMessage()
                );
            }
        });

        connection.start();
    }

    public Integer getLatestLevel() {
        return latestLevel.get();
    }

    @Override
    public void close() {
        try {
            consumer.close();
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
