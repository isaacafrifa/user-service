package iam.userservice.config.rabbitmq;

import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.AmqpTemplate;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    /* Create Queue */
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(queueName)
                // When messages fail, send to default exchange
                .withArgument("x-dead-letter-exchange", "")
                // Route failed messages to a queue named "<originalQueue>.dlq"
                .withArgument("x-dead-letter-routing-key", queueName + ".dlq")
                .build();
    }

    /**
     * Dead Letter Queue (DLQ) Configuration
     * Purpose:
     * - Captures messages that fail to process in the main queue
     * - Acts as a message parking lot for failed/rejected messages
     * - Enables message inspection and troubleshooting
     * - Allows manual reprocessing of failed messages
     * <p>
     * Messages are sent to DLQ when:
     * - Message processing fails (consumer rejection)
     * - Message expires (TTL exceeded)
     * - Queue length limit reached
     * - Message is negatively acknowledged
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(queueName + ".dlq")
                .build();
    }

    /* Create Exchange */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    /* Bind queue to topic exchange with binding key */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(routingKey);
    }

    //convert message to JSON to be sent to a particular queue
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    //Create RabbitMQ template
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

}