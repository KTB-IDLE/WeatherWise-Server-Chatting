package com.idle.weather.chatting.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.kafka.request.KafkaChatMessageRequest;
import com.idle.weather.chatting.message.api.port.ChatMessageService;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.websocket.WebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final WebSocketBroadcaster broadcaster;

    @KafkaListener(topics = "chatting-topic", groupId = "chatting-group")
    public void consumeMessage(String message) {
        try {
            KafkaChatMessageRequest kafkaRequest = objectMapper.readValue(message, KafkaChatMessageRequest.class);

            // KafkaChatMessageRequest -> ChatMessageRequest 변환
            ChatMessageRequest chatMessageRequest = ChatMessageRequest.from(kafkaRequest);

            // Redis 및 DB에 메시지 저장
            chatMessageService.sendMessage(kafkaRequest.chatRoomId(), kafkaRequest.userId(), chatMessageRequest)
                    .doOnNext(response -> {
                        // 저장된 메시지를 WebSocket 브로드캐스트
                        broadcaster.broadcast(kafkaRequest.chatRoomId(), response.message());
                    })
                    .doOnError(e -> log.error("DB/Redis 저장 실패", e))
                    .subscribe();

        } catch (Exception e) {
            log.error("Kafka 메시지 소비 실패: {}", e.getMessage());
        }
    }

}