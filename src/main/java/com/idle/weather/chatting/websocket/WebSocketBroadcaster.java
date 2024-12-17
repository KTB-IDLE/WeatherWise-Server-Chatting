package com.idle.weather.chatting.websocket;

import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketBroadcaster {

    private final Map<Long, Map<String, WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

    public void addSession(Long chatRoomId, WebSocketSession session) {
        chatRoomSessions.computeIfAbsent(chatRoomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
    }

    public void broadcast(Long chatRoomId, String message) {
        chatRoomSessions.getOrDefault(chatRoomId, Map.of()).values().forEach(session ->
                session.send(Mono.just(session.textMessage(message)))
                        .doOnError(e -> System.out.println("Broadcast error: " + e.getMessage()))
                        .subscribe()
        );
    }
}
