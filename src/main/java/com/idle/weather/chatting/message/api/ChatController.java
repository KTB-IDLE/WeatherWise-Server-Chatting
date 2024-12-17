package com.idle.weather.chatting.message.api;

import com.idle.weather.chatting.message.api.port.ChatMessageService;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/send/{chatRoomId}")
    public Mono<ChatMessageResponse> sendMessage(@PathVariable Long chatRoomId, @RequestBody ChatMessageRequest chatMessageRequest, @RequestHeader("userId") Long senderId) {
        return chatMessageService.sendMessage(chatRoomId, senderId, chatMessageRequest);
    }

    @GetMapping("/{chatRoomId}/recent")
    public Flux<ChatMessageResponse> getRecentMessages(@PathVariable Long chatRoomId) {
        return chatMessageService.getRecentMessages(chatRoomId);
    }

    @GetMapping("/{chatRoomId}")
    public Flux<ChatMessageResponse> getMessages(@PathVariable Long chatRoomId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return chatMessageService.getMessages(chatRoomId, page, size);
    }
}
