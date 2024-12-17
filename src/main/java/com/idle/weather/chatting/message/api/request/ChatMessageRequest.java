package com.idle.weather.chatting.message.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChatMessageRequest(
        @Schema(description = "메시지 내용", example = "비가 너무 많이오네요.. 고양이들 괜찮을까요?")
        String message
) {
        public ChatMessageRequest {
                if (message == null || message.trim().isEmpty()) {
                        throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
                }
        }
}
