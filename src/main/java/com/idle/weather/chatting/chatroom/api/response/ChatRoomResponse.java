package com.idle.weather.chatting.chatroom.api.response;

import com.idle.weather.chatting.chatroom.repository.ReactiveChatRoomEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        @Schema(description = "채팅방 아이디", example = "1")
        Long id,

        @Schema(description = "채팅방 이름", example = "서울 지진 경보 채팅방")
        String name,

        @Schema(description = "기상특보 지역 코드", example = "S1250000")
        String parentRegionCode,

        @Schema(description = "기상특보 지역 이름", example = "서해중부전해상")
        String parentRegionName,

        @Schema(description = "채팅방 활성화 여부", example = "1")
        boolean isActivated,

        @Schema(description = "채팅방 비활성화된 시점", example = "202412021158")
        LocalDateTime deactivatedAt
) {
    public static ChatRoomResponse from(ReactiveChatRoomEntity chatRoomEntity) {
        return new ChatRoomResponse(
                chatRoomEntity.getId(),
                chatRoomEntity.getName(),
                chatRoomEntity.getParentRegionCode(),
                chatRoomEntity.getParentRegionName(),
                chatRoomEntity.isActivated(),
                chatRoomEntity.getDeactivatedAt()
        );
    }

    public ReactiveChatRoomEntity toEntity() {
        return ReactiveChatRoomEntity.builder()
                .id(this.id)
                .name(this.name)
                .parentRegionCode(this.parentRegionCode)
                .parentRegionName(this.parentRegionName)
                .isActivated(this.isActivated)
                .deactivatedAt(this.deactivatedAt)
                .build();
    }
}
