package com.idle.weather.chatting.message.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

public interface ChatMessageR2dbcRepository extends R2dbcRepository<ReactiveChatMessageEntity, Long> {
    @Query("""
            SELECT id, chat_room_id, sender_id, message, timestamp
            FROM chat_message
            WHERE chat_room_id = :chatRoomId
            ORDER BY timestamp DESC
            """)
    Flux<ReactiveChatMessageEntity> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
