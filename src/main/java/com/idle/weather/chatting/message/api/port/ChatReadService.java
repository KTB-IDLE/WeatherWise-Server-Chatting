package com.idle.weather.chatting.message.api.port;

import com.idle.weather.chatting.message.api.response.ChatReadStatusResponse;
import com.idle.weather.chatting.message.api.response.ChatLastReadMessageResponse;
import reactor.core.publisher.Mono;

public interface ChatReadService {
    Mono<ChatReadStatusResponse> markAsRead(Long chatRoomId, Long userId, Long chatMessageId);
    Mono<ChatLastReadMessageResponse> getLastReadMessageId(Long chatRoomId, Long userId);
}
