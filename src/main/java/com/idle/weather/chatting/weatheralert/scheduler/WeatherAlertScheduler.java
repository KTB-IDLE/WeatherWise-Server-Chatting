package com.idle.weather.chatting.weatheralert.scheduler;

import com.idle.weather.chatting.chatroom.api.port.ChatRoomService;
import com.idle.weather.chatting.weatheralert.api.port.WeatherAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherAlertScheduler {

    private final WeatherAlertService weatherAlertService;
    private final ChatRoomService chatRoomService;

    // 1시간마다 기상특보 업데이트
    @Scheduled(fixedRate = 3600000)
    public void scheduleUpdateWeatherAlerts() {
        log.info("스케줄러 : 기상특보 데이터 업데이트 시작");
        weatherAlertService.updateWeatherAlerts()
                        .doOnError(e -> log.error("기상특보 데이터 업데이트 중 에러 발생", e))
                        .subscribe(); // 비동기 실행
        log.info("스케줄러 : 기상특보 데이터 업데이트 완료");
    }

    // 3시간마다 만료된 기상특보 비활성화
    @Scheduled(fixedRate = 10800000)
    public void scheduleDeactivateExpiredAlerts() {
        log.info("스케줄러 : 만료된 기상특보 비활성화 시작");
        weatherAlertService.deactivateExpiredAlerts()
                .doOnError(e -> log.error("만료된 기상특보 비활성화 중 에러 발생", e))
                .subscribe();
        log.info("스케줄러 : 만료된 기상특보 비활성화 완료");
    }

    // 매일 자정에 오래된 비활성화된 기상특보 및 채팅방 삭제
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleDeleteOldInactiveAlerts() {
        log.info("스케줄러 : 오래된 비활성화 기상특보 및 채팅방 삭제 시작");
        weatherAlertService.deleteOldDeactivatedAlerts(7)
                        .then(chatRoomService.deleteOldDeactivatedChatRooms(7))
                        .doOnError(e -> log.error("오래된 비활성화 기상특보 및 채팅방 삭제 중 에러 발생", e))
                        .subscribe();
        log.info("스케줄러 : 오래된 비활성화 기상특보 삭제 완료");
    }

}
