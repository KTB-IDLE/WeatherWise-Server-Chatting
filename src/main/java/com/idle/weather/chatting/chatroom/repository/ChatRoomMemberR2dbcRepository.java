package com.idle.weather.chatting.chatroom.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChatRoomMemberR2dbcRepository extends R2dbcRepository<ReactiveChatRoomMemberEntity, Long> {

    @Query("""
            DELETE
            FROM chat_room_member
            WHERE chat_room_id = :chatRoomId AND user_id = :userId
            """)
    Mono<Void> deleteByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("""
            SELECT CASE
            WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM chat_room_member
            WHERE chat_room_id = :chatRoomId AND user_id = :userId
            """)
    Mono<Boolean> existsByChatRoomIdAndUserId(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("""
            SELECT user_id
            FROM chat_room_member
            WHERE chat_room_id = :chatRoomId
            """)
    Flux<Long> findUserIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
