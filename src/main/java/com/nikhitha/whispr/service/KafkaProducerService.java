package com.nikhitha.whispr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic.messages}")
    private String messagesTopic;

    public void publishMessageEvent(String payload) {
        logger.debug("Publishing message to topic {}: {}", messagesTopic, payload);
        kafkaTemplate.send(messagesTopic, payload);
    }
}
