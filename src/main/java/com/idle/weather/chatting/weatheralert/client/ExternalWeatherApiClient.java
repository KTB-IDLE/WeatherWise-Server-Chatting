package com.idle.weather.chatting.weatheralert.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.weatheralert.repository.ReactiveWeatherAlertEntity;
import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalWeatherApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${weather.api.auth-key}")
    private String authKey;

    public Flux<ReactiveWeatherAlertEntity> fetchWeatherAlerts() {
        return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("apihub.kma.go.kr")
                            .path("/api/typ01/url/wrn_now_data.php")
                            .queryParam("fe", "f")
                            .queryParam("disp", 0)
                            .queryParam("authKey", authKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60)) // 호출 타임아웃 적용
                    .flatMapMany(this::parseWeatherAlerts)
                    .doOnSubscribe(sub -> log.info("기상특보 API 호출 시작..."))
                    .doOnNext(res -> log.info("기상특보 API 호출 성공"))
                    .doOnError(e -> log.error("기상특보 API 호출 중 에러 발생", e))
                    .onErrorResume(e -> {
                        if (e instanceof TimeoutException) {
                            log.error("API 호출이 타임아웃되었습니다.");
                        }
                        return Flux.empty(); // 에러 발생 시 빈 결과 반환
                    });
    }

    private Flux<ReactiveWeatherAlertEntity> parseWeatherAlerts(String response) {
        String[] lines = response.split("\n");
        return Flux.fromArray(lines)
                .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                .flatMap(line -> {
                    String[] fields = line.split(",");  // ','로 데이터 구분
                    if (fields.length < 7) {
                        log.warn("필드 개수가 2개 이상 부족한 데이터 무시: {}", line);
                        return Flux.empty();
                    }

                    try {
                        String alertLevel = fields[7].trim();// '경보', '주의', '예비' 중 하나

//                        if (!"경보".equals(alertLevel)) {
//                            return Flux.empty();
//                        }

                        ReactiveWeatherAlertEntity weatherAlert = ReactiveWeatherAlertEntity.createWeatherAlert(
                                fields[0].trim(), // parentRegionCode
                                fields[1].trim(), // parentRegionName
                                fields[2].trim(), // regionCode
                                fields[3].trim(), // regionName
                                fields[4].trim(), // announcementTime
                                fields[5].trim(), // effectiveTime
                                fields[6].trim(), // alertType
                                alertLevel,        // alertLevel
                                fields[8].trim(), // command
                                parseEndTime(fields[9].trim()), // endTime
                                null               // chatRoomId (추가 로직에서 설정)
                        );
                        return Flux.just(weatherAlert);
                    } catch (Exception e) {
                        log.warn("기상특보 데이터 파싱 실패: {}, 오류: {}", line, e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private LocalDateTime parseEndTime(String edTm) {
        if (edTm == null || edTm.isEmpty() || edTm.equals(" ") || edTm.equals("00일")) {
            return null; // 해제 예고 시점이 없을 경우
        }

        try {
            Pattern pattern = Pattern.compile("(\\d+)일\\s*(.*)");
            Matcher matcher = pattern.matcher(edTm);
            if (matcher.matches()) {
                int dayOffset = Integer.parseInt(matcher.group(1).trim());
                String timeRange = matcher.group(2).trim();

                LocalDate today = LocalDate.now();
                LocalDate targetDate = today.plusDays(dayOffset - 1);

                LocalTime time = mapTimeRangeToLocalTime(timeRange);

                return LocalDateTime.of(targetDate, time);
            } else {
                log.warn("예상치 못한 edTm 형싱 : '{}'", edTm);
                return null;
            }
        } catch (Exception e) {
            log.warn("endTime 파싱 실패: '{}', 오류: {}", edTm, e.getMessage());
            return null;
        }
    }

    private LocalTime mapTimeRangeToLocalTime(String timeRange) {
        return switch (timeRange) {
            case "새벽(00시~03시)" -> LocalTime.of(3, 0);
            case "새벽(03시~06시)" -> LocalTime.of(6, 0);
            case "아침(06시~09시)" -> LocalTime.of(9, 0);
            case "오전(09시~12시)" -> LocalTime.of(12, 0);
            case "낮(12시~15시)" -> LocalTime.of(13, 30);
            case "오후(12시~18시)" -> LocalTime.of(15, 0);
            case "늦은 오후(15시~18시)" -> LocalTime.of(18, 0);
            case "저녁(18시~21시)" -> LocalTime.of(21, 0);
            case "밤(21시~24시)" -> LocalTime.of(0, 0);
            default -> {
                log.warn("예상치 못한 시간대: '{}'", timeRange);
                yield LocalTime.of(12, 0); // 기본값 설정
            }
        };
    }
}
