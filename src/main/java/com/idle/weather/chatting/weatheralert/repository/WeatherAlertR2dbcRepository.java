package com.idle.weather.chatting.weatheralert.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface WeatherAlertR2dbcRepository extends R2dbcRepository<ReactiveWeatherAlertEntity, Long> {

    @Query("""
            SELECT *
            FROM weather_alert
            WHERE end_time IS NOT NULL AND end_time < NOW()
            """)
    Flux<ReactiveWeatherAlertEntity> findExpiredAlerts();

    @Query("""
            SELECT *
            FROM weather_alert
            WHERE is_activated = false AND deactivated_at IS NOT NULL AND deactivated_at < :cutoffDate
            """)
    Flux<ReactiveWeatherAlertEntity> findAllDeactivatedOlderThan(@Param("cutoffDate") LocalDateTime cutOffDate);

    @Query("""
            SELECT *
            FROM weather_alert
            WHERE is_activated = true
            """)
    Flux<ReactiveWeatherAlertEntity> findAllActivatedAlerts();

    @Query("""
            SELECT *
            FROM weather_alert
            WHERE chat_room_id = :chatRoomId AND is_activated = true
            """)
    Flux<ReactiveWeatherAlertEntity> findByChatRoomIdAndIsActivatedTrue(@Param("chatRoomId") Long chatRoomId);

}
