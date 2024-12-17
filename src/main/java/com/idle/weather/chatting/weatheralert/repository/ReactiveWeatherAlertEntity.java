package com.idle.weather.chatting.weatheralert.repository;

import com.idle.weather.chatting.global.BaseR2dbcEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;

@Table("weather_alert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ReactiveWeatherAlertEntity extends BaseR2dbcEntity {

    @Id
    private Long id;

    @Column("parent_region_code")
    private String parentRegionCode; // 상위 특보 구역 코드

    @Column("parent_region_name")
    private String parentRegionName; // 상위 특보 구역 이름

    @Column("region_code")
    private String regionCode; // 특보 구역 코드

    @Column("region_name")
    private String regionName; // 특보 구역 이름

    @Column("announcement_time")
    private String announcementTime; // 발표 시각

    @Column("effective_time")
    private String effectiveTime; // 발효 시각

    @Column("alert_type")
    private String alertType; // 특보 종류

    @Column("alert_level")
    private String alertLevel; // 특보 수준

    @Column("command")
    private String command; // 특보 명령

    @Column("end_time")
    private LocalDateTime endTime; // 해제 예고 시점

    @Column("is_activated")
    private boolean isActivated; // 기상특보 해제 여부

    @Column("deactivated_at")
    private LocalDateTime deactivatedAt; // 비활성화 시점

    @Column("chat_room_id")
    private Long chatRoomId;

    public static ReactiveWeatherAlertEntity createWeatherAlert(
            String parentRegionCode,
            String parentRegionName,
            String regionCode,
            String regionName,
            String announcementTime,
            String effectiveTime,
            String alertType,
            String alertLevel,
            String command,
            LocalDateTime endTime,
            Long chatRoomId
    ) {
        return ReactiveWeatherAlertEntity.builder()
                .parentRegionCode(parentRegionCode)
                .parentRegionName(parentRegionName)
                .regionCode(regionCode)
                .regionName(regionName)
                .announcementTime(announcementTime)
                .effectiveTime(effectiveTime)
                .alertType(alertType)
                .alertLevel(alertLevel)
                .command(command)
                .endTime(endTime)
                .isActivated(true)
                .chatRoomId(chatRoomId)
                .build();
    }

    public void updateWeatherAlert(ReactiveWeatherAlertEntity apiAlert) {
        this.endTime = apiAlert.getEndTime();
        this.command = apiAlert.getCommand();
        this.alertLevel = apiAlert.getAlertLevel();
    }

    public void deactivateWeatherAlert() {
        if (this.isActivated) {
            this.isActivated = false;
            this.deactivatedAt = LocalDateTime.now();
        }
    }

    public void updateChatRoomId(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
}
