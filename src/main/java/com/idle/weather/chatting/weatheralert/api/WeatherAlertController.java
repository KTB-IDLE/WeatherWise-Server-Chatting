package com.idle.weather.chatting.weatheralert.api;

import com.idle.weather.chatting.weatheralert.api.port.WeatherAlertService;
import com.idle.weather.chatting.weatheralert.api.response.WeatherAlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather-alerts")
public class WeatherAlertController {

    private final WeatherAlertService weatherAlertService;

    // 모든 기상특보 조회
    @GetMapping
    public Flux<WeatherAlertResponse> getAllWeatherAlert() {
        return weatherAlertService.getAllWeatherAlerts();
    }

    // 특정 기상특보 조회
    @GetMapping("/{weatherAlertId}")
    public Mono<ResponseEntity<WeatherAlertResponse>> getWeatherAlertById(@PathVariable Long weatherAlertId) {
        return weatherAlertService.getWeatherAlertById(weatherAlertId)
                .map(ResponseEntity::ok);
    }

    // 활성화된 기상특보만 조회
    @GetMapping("/activated")
    public Flux<WeatherAlertResponse> getAllActivatedWeatherAlerts() {
        return weatherAlertService.getAllActivatedWeatherAlerts();
    }

    // 기상특보 업데이트 수동 트리거 (테스트용)
    @PostMapping("/update")
    public Mono<ResponseEntity<Void>> updateWeatherAlerts() {
        return weatherAlertService.updateWeatherAlerts()
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    // 만료된 기상특보 비활성화 수동 트리거 (테스트용)
    @PostMapping("/deactivate-expired")
    public Mono<ResponseEntity<Void>> deactivateExpiredAlerts() {
        return weatherAlertService.deactivateExpiredAlerts()
                .thenReturn(ResponseEntity.ok().<Void>build());
    }

    // 오래된 비활성화된 기상특보 삭제 수동 트리거 (테스트용)
    @DeleteMapping("/delete-old")
    public Mono<ResponseEntity<Void>> deleteOldDeactivatedAlerts(@RequestParam(defaultValue = "7") int day) {
        return weatherAlertService.deleteOldDeactivatedAlerts(day)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }

}
