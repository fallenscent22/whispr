package com.nikhitha.whispr.service;

import com.nikhitha.whispr.dto.ChatMessage;
import com.nikhitha.whispr.entity.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.kafka.topic.messages}")
    private String messagesTopic;

    @Value("${app.kafka.consumer.group}")
    private String consumerGroup;

    @KafkaListener(topics = "${app.kafka.topic.messages}", groupId = "${app.kafka.consumer.group}")
    public void listen(String message) {
        logger.debug("Received kafka message on topic {}: {}", messagesTopic, message);
        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);

            // Save message and use the returned entity for additional processing
            Message savedMessage = messageService.saveMessage(chatMessage);

            // Log the saved message ID for tracking
            logger.debug("Message saved with ID: {}", savedMessage.getId());

            String destination = "/topic/public";
            if (chatMessage.getRoomId() != null && !chatMessage.getRoomId().equals("global")) {
                destination = "/topic/room." + chatMessage.getRoomId();
            }

            // Enhance the chatMessage with the saved message ID
            chatMessage.setMessageId(savedMessage.getId());
            messagingTemplate.convertAndSend(destination, chatMessage);
            logger.debug("Broadcasted message to: {}", destination);

        } catch (Exception e) {
            logger.error("Failed to process Kafka message: {}", e.getMessage(), e);
        }
    }
}