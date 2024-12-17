package com.idle.weather.chatting.chatroom.api;


import com.idle.weather.chatting.chatroom.api.port.ChatRoomService;
import com.idle.weather.chatting.chatroom.api.request.CreateChatRoomRequest;
import com.idle.weather.chatting.chatroom.api.response.ChatRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 활성화된 모든 채팅방 조회
    @GetMapping("/activated")
    public Flux<ChatRoomResponse> getAllActivatedChatRooms() {
        return chatRoomService.getAllActivatedChatRooms();
    }

    // 채팅방 생성
    @PostMapping
    public Mono<ResponseEntity<ChatRoomResponse>> createChatRoom(@RequestBody CreateChatRoomRequest createChatRoomRequest) {
        return chatRoomService.getOrCreateChatRoom(createChatRoomRequest.parentRegionCode(), createChatRoomRequest.parentRegionName())
                .map(chatRoom -> ResponseEntity.ok(ChatRoomResponse.from(chatRoom)));
    }

    // 특정 채팅방 삭제
    @DeleteMapping("/{chatRoomId}")
    public Mono<ResponseEntity<Void>> deleteChatRoom(@PathVariable Long chatRoomId) {
        return chatRoomService.deleteChatRoomById(chatRoomId)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    // 특정 채팅방 조회
    @GetMapping("/{chatRoomId}")
    public Mono<ResponseEntity<ChatRoomResponse>> getChatRoom(@PathVariable Long chatRoomId) {
        return chatRoomService.getChatRoomById(chatRoomId)
                .map(chatRoom -> ResponseEntity.ok(ChatRoomResponse.from(chatRoom)));
    }

    // 모든 채팅방 조회
    @GetMapping("/all")
    public Flux<ChatRoomResponse> getAllChatRooms() {
        return chatRoomService.getAllChatRooms();
    }

    // 오래된 비활성화된 기상특보 삭제 수동 트리거 (테스트용)
    @DeleteMapping("/delete-old")
    public Mono<ResponseEntity<Void>> deleteOldDeactivatedChatRooms(@RequestParam(defaultValue = "7") int day) {
        return chatRoomService.deleteOldDeactivatedChatRooms(day)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

}
