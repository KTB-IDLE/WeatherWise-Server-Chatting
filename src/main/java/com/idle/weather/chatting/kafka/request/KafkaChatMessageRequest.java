package com.idle.weather.chatting.kafka.request;

import lombok.Builder;

@Builder
public record KafkaChatMessageRequest(
        Long chatRoomId,
        Long userId,
        String message
) {
    public KafkaChatMessageRequest {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }
    }
}
