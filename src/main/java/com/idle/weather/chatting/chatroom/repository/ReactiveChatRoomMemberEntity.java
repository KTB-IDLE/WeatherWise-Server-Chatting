package com.idle.weather.chatting.chatroom.repository;

import com.idle.weather.chatting.global.BaseR2dbcEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

@Table(name = "chat_room_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReactiveChatRoomMemberEntity extends BaseR2dbcEntity {

    @Id
    private Long id;

    @Column("chat_room_id")
    private Long chatRoomId;

    @Column("user_id")
    private Long userId;

    public static ReactiveChatRoomMemberEntity createChatRoomMember(Long chatRoomId, Long userId) {
        return ReactiveChatRoomMemberEntity.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .build();
    }
}
