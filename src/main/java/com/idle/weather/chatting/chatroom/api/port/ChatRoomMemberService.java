package com.idle.weather.chatting.chatroom.api.port;

import com.idle.weather.chatting.chatroom.api.response.ChatRoomMemberResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomMemberService {
    Mono<ChatRoomMemberResponse> joinChatRoom(Long chatRoomId, Long userId);
    Mono<Void> leaveChatRoom(Long chatRoomId, Long userId);
    Flux<Long> getChatRoomUsers(Long chatRoomId);
}
