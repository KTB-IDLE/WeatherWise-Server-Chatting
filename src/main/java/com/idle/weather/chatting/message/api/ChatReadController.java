package com.idle.weather.chatting.message.api;

import com.idle.weather.chatting.message.api.port.ChatReadService;
import com.idle.weather.chatting.message.api.response.ChatLastReadMessageResponse;
import com.idle.weather.chatting.message.api.response.ChatReadStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/read")
public class ChatReadController {

    private final ChatReadService chatReadService;

    @PostMapping("/{chatRoomId}/message/{chatMessageId}")
    public Mono<ChatReadStatusResponse> markAsRead(@PathVariable Long chatRoomId, @RequestHeader("userId") Long userId, @PathVariable Long chatMessageId) {
        return chatReadService.markAsRead(chatRoomId, userId, chatMessageId);
    }

    @GetMapping("/{chatRoomId}/last-read")
    public Mono<ChatLastReadMessageResponse> getLastReadMessage(@PathVariable Long chatRoomId, @RequestHeader("userId") Long userId) {
        return chatReadService.getLastReadMessageId(chatRoomId, userId);
    }
}
