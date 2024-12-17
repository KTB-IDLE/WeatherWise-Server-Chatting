package com.idle.weather.chatting.chatroom.api;

import com.idle.weather.chatting.chatroom.api.port.ChatRoomMemberService;
import com.idle.weather.chatting.chatroom.api.response.ChatRoomMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat/{chatRoomId}/members")
@RequiredArgsConstructor
public class ChatRoomMemberController {
    private final ChatRoomMemberService chatRoomMemberService;

    @PostMapping("/join")
    public Mono<ResponseEntity<ChatRoomMemberResponse>> joinChatRoom(@PathVariable Long chatRoomId, @RequestHeader("userId") Long userId) {
        return chatRoomMemberService.joinChatRoom(chatRoomId, userId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/leave")
    public Mono<ResponseEntity<Void>> leaveChatRoom(@PathVariable Long chatRoomId, @RequestHeader("userId") Long userId) {
        return chatRoomMemberService.leaveChatRoom(chatRoomId, userId)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @GetMapping
    public Flux<Long> getChatRoomUsers(@PathVariable Long chatRoomId) {
        return chatRoomMemberService.getChatRoomUsers(chatRoomId);
    }
}
