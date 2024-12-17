package com.idle.weather.chatting.message.service;

import com.idle.weather.chatting.message.api.port.ChatReadService;
import com.idle.weather.chatting.message.api.response.ChatLastReadMessageResponse;
import com.idle.weather.chatting.message.api.response.ChatReadStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveChatReadServiceImpl implements ChatReadService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final String READ_STATUS_KEY_FORMAT = "chatroom:%d:read_status:user:%d";

    @Override
    public Mono<ChatReadStatusResponse> markAsRead(Long chatRoomId, Long userId, Long chatMessageId) {
        if (chatRoomId == null || userId == null || chatMessageId == null) {
            return Mono.error(new IllegalArgumentException("ChatRoomId, UserId, and MessageId must not be null"));
        }

        String key = String.format(READ_STATUS_KEY_FORMAT, chatRoomId, userId);
        return reactiveRedisTemplate.opsForValue()
                .set(key, String.valueOf(chatMessageId))
                .flatMap(success -> {
                    if (success) {
                        return Mono.just(new ChatReadStatusResponse(chatRoomId, userId, chatMessageId, LocalDateTime.now(), "Marked as read successfully"));
                    } else {
                        return Mono.error(new RuntimeException("Failed to mark message as read"));
                    }
                });
    }

    @Override
    public Mono<ChatLastReadMessageResponse> getLastReadMessageId(Long chatRoomId, Long userId) {
        if (chatRoomId == null || userId == null) {
            return Mono.error(new IllegalArgumentException("ChatRoomId and UserId must not be null"));
        }

        String key = String.format(READ_STATUS_KEY_FORMAT, chatRoomId, userId);
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .flatMap(lastReadMessageIdStr -> {
                    try {
                        Long lastReadMessageId = Long.parseLong(lastReadMessageIdStr);
                        return Mono.just(new ChatLastReadMessageResponse(chatRoomId, userId, lastReadMessageId));
                    } catch (NumberFormatException e) {
                        return Mono.error(new RuntimeException("Invalid last read message ID format", e));
                    }
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No read status found for the given user in the chat room")));
    }
}
