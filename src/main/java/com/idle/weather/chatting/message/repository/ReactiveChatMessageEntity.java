package com.idle.weather.chatting.message.repository;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.idle.weather.chatting.global.BaseR2dbcEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReactiveChatMessageEntity extends BaseR2dbcEntity {

    @Id
    private Long id;

    @Column("chat_room_id")
    private Long chatRoomId;

    @Column("sender_id")
    private Long senderId; // 보낸 사람

    @Column("message")
    private String message; // 메시지 내용

    @Column("timestamp")
    private LocalDateTime timestamp; // 전송시간

    @Builder
    private ReactiveChatMessageEntity(Long chatRoomId, Long senderId, String message, LocalDateTime timestamp) {
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    @JsonCreator
    public static ReactiveChatMessageEntity jsonCreator(
            @JsonProperty("chatRoomId") Long chatRoomId,
            @JsonProperty("sendId") Long senderId,
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") LocalDateTime timestamp
    ) {
        return new ReactiveChatMessageEntity(chatRoomId, senderId, message, timestamp);
    }

    public static ReactiveChatMessageEntity createChatMessage(Long chatRoomId, Long senderId, String message) {
        return ReactiveChatMessageEntity.builder()
                .chatRoomId(chatRoomId)
                .senderId(senderId)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

