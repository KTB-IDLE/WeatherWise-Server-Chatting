package com.idle.weather.chatting.chatroom.api.response;

import com.idle.weather.chatting.chatroom.repository.ReactiveChatRoomMemberEntity;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChatRoomMemberResponse(
        @Schema(description = "채팅방-사용자 관계 ID", example = "1")
        Long id,

        @Schema(description = "채팅방 ID", example = "1")
        Long chatRoomId,

        @Schema(description = "사용자 ID", example = "1")
        Long userId
) {
    public static ChatRoomMemberResponse from(ReactiveChatRoomMemberEntity chatRoomMember) {
        return new ChatRoomMemberResponse(
                chatRoomMember.getId(),
                chatRoomMember.getChatRoomId(),
                chatRoomMember.getUserId()
        );
    }
}
