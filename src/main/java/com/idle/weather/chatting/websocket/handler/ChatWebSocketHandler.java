package com.idle.weather.chatting.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.kafka.producer.ChatMessageProducer;
import com.idle.weather.chatting.kafka.request.KafkaChatMessageRequest;
import com.idle.weather.chatting.message.api.port.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final ChatMessageProducer kafkaProducer;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> processMessage(payload, session));

        return session.send(output);
    }

    private Mono<WebSocketMessage> processMessage(String payload, WebSocketSession session) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, KafkaChatMessageRequest.class))
                .doOnNext(request -> kafkaProducer.sendMessage(request)) // kafka 메시지 전송
                .map(request -> {
                    String msg = "메시지 전송 성공: " + request.message();
                    return session.textMessage(msg); // WebSocketMessage 만들어서 리턴
                })
                .onErrorResume(e -> {
                    log.error("Kafka - 메시지 처리 실패: {}", e.getMessage());
                    return Mono.just(session.textMessage("에러 발생: " + e.getMessage()));
                });
    }
}
