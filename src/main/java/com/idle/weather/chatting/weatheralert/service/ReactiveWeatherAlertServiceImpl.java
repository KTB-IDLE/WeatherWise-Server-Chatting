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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
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

    @Override
    public Mono<Void> updateWeatherAlerts() {
        log.info("기상특보 업데이트 시작 ...");
        return externalWeatherApiClient.fetchWeatherAlerts()
                .collectList()
                .flatMap(apiAlerts -> weatherAlertRepository.findAllActivatedAlerts()
                        .collectList()
                        .flatMap(dbAlerts -> {
                            // DB 기상 특보를 Map으로 변환
                            Map<String, ReactiveWeatherAlertEntity> dbAlertMap = dbAlerts.stream()
                                    .collect(Collectors.toMap(this::generateUniqueKey, alert -> alert));

                            Flux<Void> saveOrUpdateFlux = Flux.fromIterable(apiAlerts)
                                    .flatMap(apiAlert -> {
                                        String apiAlertKey = generateUniqueKey(apiAlert);
                                        ReactiveWeatherAlertEntity dbAlert = dbAlertMap.get(apiAlertKey);





                                        if (dbAlert == null) {
                                            // 새로운 기상특보 저장
                                            return chatRoomService.getOrCreateChatRoom(apiAlert.getParentRegionCode(), apiAlert.getParentRegionName())
                                                    .flatMap(chatRoom -> {
                                                        apiAlert.updateChatRoomId(chatRoom.getId()); // chatRoomId 설정
                                                        return weatherAlertRepository.save(apiAlert);
                                                    })
                                                    .doOnNext(savedAlert -> log.info("새로운 기상특보 추가됨: {}", savedAlert))
                                                    .flatMap(savedAlert -> chatRoomService.updateChatRoomName(savedAlert.getChatRoomId()));
                                        } else {
                                            // 기존 기상특보 업데이트
                                            if (hasAlertChanged(dbAlert, apiAlert)) {
                                                dbAlert.updateWeatherAlert(apiAlert);
                                                return weatherAlertRepository.save(dbAlert)
                                                        .doOnNext(updatedAlert -> log.info("기상특보 업데이트됨: {}", updatedAlert))
                                                        .flatMap(updatedAlert -> {
                                                            // 채팅방 이름 업데이트
                                                            return chatRoomService.getChatRoomById(updatedAlert.getChatRoomId())
                                                                    .flatMap(chatRoom -> {
                                                                        return chatRoomService.updateChatRoomName(updatedAlert.getChatRoomId());
                                                                    })
                                                                    .then();
                                                        });
                                            } else {
                                                return Mono.empty(); // 변경 없음
                                            }
                                        }
                                    });

                            // API에 없는 DB의 기상특보 비활성화
                            Flux<Void> deactivateFlux = Flux.fromIterable(dbAlertMap.values())
                                    .flatMap(dbAlert -> {
                                        String dbAlertKey = generateUniqueKey(dbAlert);
                                        boolean existsInApi = apiAlerts.stream()
                                                .anyMatch(apiAlert -> generateUniqueKey(apiAlert).equals(dbAlertKey));
                                        if (!existsInApi) {
                                            dbAlert.deactivateWeatherAlert();
                                            return weatherAlertRepository.save(dbAlert)
                                                    .doOnNext(deactivatedAlert -> log.info("기상특보 비활성화 : {}", deactivatedAlert))
                                                    .flatMap(deactivatedAlert -> {
                                                        // 채팅방 이름 업데이트 또는 비활성화
                                                        return chatRoomService.getChatRoomById(deactivatedAlert.getChatRoomId())
                                                                .flatMap(chatRoom -> {
                                                                    // 현재 채팅방에 활성화된 기상특보가 더 이상 없는지 확인
                                                                    return weatherAlertRepository.findByChatRoomIdAndIsActivatedTrue(deactivatedAlert.getChatRoomId())
                                                                            .hasElements()
                                                                            .flatMap(hasActiveAlerts -> {
                                                                                if (!hasActiveAlerts) {
                                                                                    // 채팅방 비활성화
                                                                                    chatRoom.deactivateChatRoom();
                                                                                    return chatRoomService.saveChatRoom(chatRoom);
                                                                                } else {
                                                                                    // 채팅방 이름 업데이트
                                                                                    return chatRoomService.updateChatRoomName(chatRoom.getId());
                                                                                }
                                                                            });
                                                                })
                                                                .then();
                                                    });
                                        } else {
                                            return Mono.empty(); // API에 존재
                                        }
                                    });

                            // 모든 추가/업데이트와 비활성화를 순차적으로 실행
                            return saveOrUpdateFlux
                                    .thenMany(deactivateFlux)
                                    .then();
                        }))
                .doOnError(e -> log.error("기상특보 업데이트 실패", e))
                .then();
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
