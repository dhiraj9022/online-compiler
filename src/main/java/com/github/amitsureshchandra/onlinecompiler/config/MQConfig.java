package com.github.amitsureshchandra.onlinecompiler.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQConfig {

    public final static String queueName = "code-queue";
    public final static String exchangeName = "exchange";

    @Bean
    Queue codeQueue() {
        return new Queue(queueName);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    Binding codeBinding(Queue codeQueue, TopicExchange exchange) {
        return BindingBuilder.bind(codeQueue).to(exchange).with("code");
    }

    @Bean
    MessageConverter msgConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public ConnectionFactory connectionFactory(@Value("${spring.rabbitmq.username}") String username, @Value("${spring.rabbitmq.password}") String password, @Value("${spring.rabbitmq.host}") String host, @Value("${spring.rabbitmq.port}") int port) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }
}
