
# WeatherWise-Server-Chatting

WeatherWise프로젝트에서 대용량 트래픽이 예상되는 실시간 기상 특보에 따른 채팅방 생성 및 채팅 기능을 분리하여 리팩토링하였습니다.  
기존 Spring MVC 기반 채팅 구조의 한계를 극복하고, **WebFlux + R2DBC + Kafka 기반 비동기/논블로킹 아키텍처**로 리팩토링하여 최대 TPS(초당 메시지 처리량)을 (400 → 3,700) 9개 향상, 동일 부하 조건에서 응답 속도(5.52초 → 0.13초) 40배 개선했습니다.

---

## 🔧 기술 스택 및 구조

- **Spring WebFlux**: 논블로킹 I/O 기반의 고성능 웹 어플리케이션 개발
- **R2DBC (MySQL)**: 논블로킹 방식의 데이터베이스 연동
- **Kafka**: 메시지 브로커로 활용하여 (3개 브로커 구성 및 파티셔닝 설정으로 메시지 안정성과 병렬 소비 구조 확보)
- **Redis**: 최신 채팅 메시지 100개 캐싱 (메시지 조회 성능 향상)
- **WebSocket**: 실시간 양방향 채팅 처리
- **Artillery**: WebSocket 부하 테스트 도구
- **Prometheus + Grafana**: 시스템 메트릭 수집 및 시각화 대시보드를 통해 성능 개선 효과 및 스레드 효율성 입증 확인
- **Docker**: Kafka(3개), Zookeeper, Prometheus, Grafana 등 인프라 전반을 로컬 컨테이너로 구성 
---

## ⚙️ 리팩토링 배경

| 항목 | 기존(Spring MVC) | 개선(WebFlux 기반) |
|------|------------------|---------------------|
| 처리 방식 | 동기/스레드 기반 | 비동기/논블로킹 |
| TPS 한계 | 400건/초에서 서버 hang 발생 | 최대 3,700건/초 이상 처리 가능 |
| 구조 | DB 직접 저장 + WebSocket 응답 | Kafka + Redis 캐싱 |
| 확장성 | 낮음 (스레드/리소스 제한) | 높음 (스케일 아웃 구조) |

---

## ✅ 리팩토링 목적 및 기술 선택 이유

### WebFlux
- 블로킹 없는 비동기 처리로 **I/O 효율성 및 동시성 향상**
- 실시간 채팅과 같이 연결이 길게 유지되어 스레드를 비효율적으로 쓰는 서비스에 최적화

### R2DBC
- 기존 JDBC의 블로킹 한계를 극복
- WebFlux와의 **완전한 논블로킹 통합**

### Kafka
- **백프레셔(Backpressure)** 대응을 위해 Kafka 기반의 비동기 메시지 파이프라인을 구축
- **Topic을 3개 파티션으로 구성**하고, `chatRoomId`를 메시지 Key로 지정하여 **채팅방 단위 메시지 순서를 보장**하면서도 **병렬 소비를 통한 확장성 확보**
- Consumer 측에서는 `@KafkaListener(concurrency = 3)` 설정을 통해 파티션 수에 맞춘 병렬 소비 구조를 구현하고, 수신한 메시지를 Redis와 DB에 저장한 후 WebSocket으로 브로드캐스트하는 로직을 구성
- 메시지 유실·중복 방지 및 장애 대응을 위해 `acks=all`, `enable.idempotence=true`, `retries=5` 설정 적용
- Kafka 브로커는 **Zookeeper + 3개 Broker로 구성된 로컬 클러스터를 Docker-Compose로 직접 구성**하여 테스트 환경에서도 **복제(replication) 기반의 안정성 및 장애 복구 시나리오**를 검증

### Redis
- 입장 시 최신 100개의 메시지를 캐싱해 **읽기 응답 성능 개선**
- DB와 분리된 구조로 실시간성 확보

---

## 📈 성능 테스트 (Artillery)

### 테스트 설정

- `arrivalRate: 1000` / `duration: 20초` / `count: 10`
- 총 **20,000명의 사용자**가 생성되며, **1인당 10개 메시지 전송** → **총 200,000건 메시지 전송**

### 결과 요약

| 지표 | 수치 |
|------|------|
| 전송된 총 메시지 수 | 200,000건 |
| 평균 처리 속도 | 3,727건/sec |
| 실패율 | 0% (완전 처리) |

---

## 🔁 메시지 흐름

```
[WebSocket Client]
      ⬇
[WebSocketHandler (Spring WebFlux)]
      ⬇
[Kafka Producer]
      ⬇
[Kafka Topic (chatting-topic, Key=chatRoomId)]
      ⬇
[Kafka Consumer]
      ⬇
[Redis (ZSet, 최근 100개 메시지 캐싱)] + [R2DBC Repository (MySQL 저장)]
      ⬇
[WebSocket Broadcaster]
      ⬇
[다른 WebSocket Clients에 메시지 전송]
```

