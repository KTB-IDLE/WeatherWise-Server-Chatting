package com.idle.weather.chatting.message.api.port;


import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatMessageService {
    Mono<ChatMessageResponse> sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest chatMessageRequest);
    Flux<ChatMessageResponse> getRecentMessages(Long chatRoomId);
    Flux<ChatMessageResponse> getMessages(Long chatRoomId, int page, int size);
}
