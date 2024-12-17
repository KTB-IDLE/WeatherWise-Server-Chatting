package com.idle.weather.chatting.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.idle.weather.chatting.message.api.port.ChatMessageService;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("Client connected: {}", sessionId);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> processMessage(payload, session))
                .doFinally(signal -> {
                    sessions.remove(sessionId);
                    log.info("Client disconnected: {}", sessionId);
                }).then();
    }

    private Mono<Void> processMessage(String payload, WebSocketSession session) {
        try {
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            Long chatRoomId = extractLongValue(messageData.get("chatRoomId"));
            Long userId = extractLongValue(messageData.get("userId"));
            String message = (String) messageData.get("message");

            ChatMessageRequest request = new ChatMessageRequest(message);

            return chatMessageService.sendMessage(chatRoomId, userId, request)
                    .doOnNext(this::broadcastMessage)
                    .onErrorResume(e -> {
                        log.error("메시지 처리 중 에러 발생: {}", e.getMessage());
                        return session.send(Mono.just(session.textMessage("메시지 처리 실패: " + e.getMessage())))
                                .then(Mono.empty());
                    })
                    .then();
        } catch (Exception e) {
            log.error("메시지 처리 실패: {}", e.getMessage());
            return session.send(Mono.just(session.textMessage("메시지 형식이 잘못되었습니다.")));
        }
    }

    private Long extractLongValue(Object value) {
        return value instanceof Number
                ? ((Number) value).longValue()
                : Long.parseLong(value.toString());
    }

    private void broadcastMessage(ChatMessageResponse response) {
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("응답 직렬화 실패: {}", e.getMessage());
            return;
        }

        String finalResponseJson = responseJson;

        Flux.fromIterable(sessions.values())
                .filter(WebSocketSession::isOpen) // 열린 세션만 필터링
                .concatMap(session ->
                        session.send(Mono.just(session.textMessage(finalResponseJson)))
                                .onErrorResume(e -> {
                                    log.error("세션 {} 전송 실패: {}", session.getId(), e.getMessage());
                                    return Mono.empty(); // 실패한 세션은 무시하고 계속 진행
                                })
                )
                .doOnComplete(() -> log.info("모든 세션에 메시지 전송 완료"))
                .subscribe();
    }
}
