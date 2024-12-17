package com.idle.weather.chatting.chatroom.repository;

import com.idle.weather.chatting.global.BaseR2dbcEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;

@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReactiveChatRoomEntity extends BaseR2dbcEntity {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("is_activated")
    private boolean isActivated;

    @Column("parent_region_code")
    private String parentRegionCode;

    @Column("parent_region_name")
    private String parentRegionName;

    @Column("deactivated_at")
    private LocalDateTime deactivatedAt;

    public static ReactiveChatRoomEntity createChatRoom(String parentRegionCode, String parentRegionName) {
        return ReactiveChatRoomEntity.builder()
                .name(parentRegionName + " 채팅방")
                .isActivated(true)
                .parentRegionCode(parentRegionCode)
                .parentRegionName(parentRegionName)
                .build();
    }

    public void deactivateChatRoom() {
        if (this.isActivated) {
            this.isActivated = false;
            this.deactivatedAt = LocalDateTime.now();
        }
    }

    public void updateName(String updatedChatRoomName) {
        this.name = updatedChatRoomName;
    }
}

