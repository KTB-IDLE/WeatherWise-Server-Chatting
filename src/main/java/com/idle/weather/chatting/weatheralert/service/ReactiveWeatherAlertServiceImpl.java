package com.idle.weather.chatting.weatheralert.service;

import com.idle.weather.chatting.chatroom.api.port.ChatRoomService;
import com.idle.weather.chatting.weatheralert.api.port.WeatherAlertService;
import com.idle.weather.chatting.weatheralert.api.response.WeatherAlertResponse;
import com.idle.weather.chatting.weatheralert.client.ExternalWeatherApiClient;
import com.idle.weather.chatting.weatheralert.repository.ReactiveWeatherAlertEntity;
import com.idle.weather.chatting.weatheralert.repository.WeatherAlertR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveWeatherAlertServiceImpl implements WeatherAlertService {

    private final ExternalWeatherApiClient externalWeatherApiClient;
    private final WeatherAlertR2dbcRepository weatherAlertRepository;
    private final ChatRoomService chatRoomService;
    private final TransactionalOperator tx;

    @Override
    public Mono<Void> updateWeatherAlerts() {
        log.info("기상특보 업데이트 시작 ...");
        return externalWeatherApiClient.fetchWeatherAlerts()
                .collectList()
                .flatMap(apiAlerts ->
                        tx.transactional(
                                synchronizeWithDatabase(apiAlerts)
                        )
                )
                .doOnError(e -> log.error("기상 특보 업데이트 실패", e))
                .then();
    }

    private Mono<Void> synchronizeWithDatabase(List<ReactiveWeatherAlertEntity> apiAlerts) {
        return weatherAlertRepository.findAllActivatedAlerts()
                .collectList()
                .flatMap(dbAlerts -> {
                    Map<String, ReactiveWeatherAlertEntity> dbMap = dbAlerts.stream()
                            .collect(Collectors.toMap(this::generateUniqueKey, alert -> alert));

                    Mono<Void> updateOrInsert = Flux.fromIterable(apiAlerts)
                            .flatMap(api -> upsertOne(api, dbMap.remove(generateUniqueKey(api))))
                            .then();

                    Mono<Void> deactivate = Flux.fromIterable(dbMap.values())
                            .flatMap(this::deactivateOne)
                            .then();

                    return updateOrInsert.then(deactivate);
                });
    }

    /** 신규 저장 또는 변경된 특보 업데이트 */
    private Mono<Void> upsertOne(ReactiveWeatherAlertEntity api, ReactiveWeatherAlertEntity db) {
        if ( db == null) {
            return chatRoomService.getOrCreateChatRoom(api.getParentRegionCode(), api.getParentRegionName())
                    .flatMap(room -> {
                        api.updateChatRoomId(room.getId());
                        return weatherAlertRepository.save(api)
                                .then(chatRoomService.updateChatRoomName(room.getId()));
                    });
        }
        if (!hasAlertChanged(db, api)) {
            return Mono.empty();
        }
        db.updateWeatherAlert(api);
        return weatherAlertRepository.save(db)
                .then(chatRoomService.updateChatRoomName(db.getChatRoomId()));
    }

    /** DB에만 존재하는 특보 비활성화 */
    private Mono<Void> deactivateOne(ReactiveWeatherAlertEntity db) {
        db.deactivateWeatherAlert();
        return weatherAlertRepository.save(db)
                .flatMap(saved -> chatRoomService.getChatRoomById(saved.getChatRoomId())
                        .flatMap(room -> weatherAlertRepository.findByChatRoomIdAndIsActivatedTrue(room.getId())
                                .hasElements()
                                .flatMap(has -> {
                                    if (!has) {
                                        room.deactivateChatRoom();
                                        return chatRoomService.saveChatRoom(room);
                                    }
                                    return chatRoomService.updateChatRoomName(room.getId());
                                })
                        )
                );
    }

    @Override
    public Mono<Void> deactivateExpiredAlerts() {
        log.info("해제 예고시점이 지난 기상특보 비활성화 시작 ...");
        return weatherAlertRepository.findExpiredAlerts()
                .flatMap(expiredAlert -> {
                    expiredAlert.deactivateWeatherAlert();
                    return weatherAlertRepository.save(expiredAlert)
                            .doOnNext(deactivatedAlert -> log.info("해제 예고시점이 지난 기상특보 비활성화 : {}", deactivatedAlert))
                            .then();
                })
                .then();
    }

    @Override
    public Mono<Void> deleteOldDeactivatedAlerts(int day) {
        log.info("오래된 비활성화 기상특보 삭제 시작 ...");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(day);
        return weatherAlertRepository.findAllDeactivatedOlderThan(cutoffDate)
                .flatMap(alert -> weatherAlertRepository.delete(alert)
                        .doOnSuccess(unused -> log.info("오래된 비활성화 기상특보 삭제 : {}",alert)))
                .then();
    }

    @Override
    public Flux<WeatherAlertResponse> getAllWeatherAlerts() {
        return weatherAlertRepository.findAll()
                .map(WeatherAlertResponse::from);
    }

    @Override
    public Flux<WeatherAlertResponse> getAllActivatedWeatherAlerts() {
        return weatherAlertRepository.findAllActivatedAlerts()
                .map(WeatherAlertResponse::from);
    }

    @Override
    public Mono<WeatherAlertResponse> getWeatherAlertById(Long weatherAlertId) {
        return weatherAlertRepository.findById(weatherAlertId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("WeatherAlert not found with id :" + weatherAlertId)))
                .map(WeatherAlertResponse::from);
    }

    private boolean hasAlertChanged(ReactiveWeatherAlertEntity dbAlert, ReactiveWeatherAlertEntity apiAlert) {
        return !Objects.equals(dbAlert.getEndTime(), apiAlert.getEndTime()) ||
                !Objects.equals(dbAlert.getCommand(), apiAlert.getCommand()) ||
                !Objects.equals(dbAlert.getAlertLevel(), apiAlert.getAlertLevel()) ||
                !Objects.equals(dbAlert.getAnnouncementTime(), apiAlert.getAnnouncementTime()) ||
                !Objects.equals(dbAlert.getEffectiveTime(), apiAlert.getEffectiveTime());
    }

    private String generateUniqueKey(ReactiveWeatherAlertEntity weatherAlert) {
        String parentRegionCode = Objects.toString(weatherAlert.getParentRegionCode(), "").trim();
        String regionCode = Objects.toString(weatherAlert.getRegionCode(), "").trim();
        String announcementTime = Objects.toString(weatherAlert.getAnnouncementTime(), "").trim();
        String effectiveTime = Objects.toString(weatherAlert.getEffectiveTime(), "").trim();
        String alertType = Objects.toString(weatherAlert.getAlertType(), "").trim();

        String uniqueKey = parentRegionCode + "|" + regionCode + "|" + announcementTime + "|" + effectiveTime + "|" + alertType;
        return DigestUtils.sha256Hex(uniqueKey);
    }
}
