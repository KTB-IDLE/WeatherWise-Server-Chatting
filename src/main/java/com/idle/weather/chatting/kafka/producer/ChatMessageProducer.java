package com.idle.weather.chatting.kafka.producer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.kafka.request.KafkaChatMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendMessage(KafkaChatMessageRequest message) {
        try {
            String payload = objectMapper.writeValueAsString(message);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("chatting-topic", payload);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka 메시지 전송 성공: topic={}, offset={}, payload={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().offset(),
                            payload);
                } else {
                    log.error("Kafka 메시지 전송 실패: {}", ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Kafka 직렬화 실패", e);
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }
}