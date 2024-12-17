package com.idle.weather.chatting.chatroom.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ChatRoomR2dbcRepository extends R2dbcRepository<ReactiveChatRoomEntity, Long> {

    @Query("""
            SELECT *
            FROM chat_room
            WHERE is_activated = true
            """)
    Flux<ReactiveChatRoomEntity> findAllActivatedChatRooms();

    @Query("""
            SELECT *
            FROM chat_room
            WHERE parent_region_code = :parentRegionCode
            LIMIT 1
            """)
    Mono<ReactiveChatRoomEntity> findByParentRegionCode(@Param("parentRegionCode") String parentRegionCode);

    @Query("""
            SELECT *
            FROM chat_room
            WHERE is_activated = false AND deactivated_at IS NOT NULL AND deactivated_at < :cutoffDate
            """)
    Flux<ReactiveChatRoomEntity> findAllDeactivatedOlderThan(@Param("cutoffDate")LocalDateTime cutoffDate);
}
