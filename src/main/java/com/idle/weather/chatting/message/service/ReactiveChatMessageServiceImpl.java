package com.idle.weather.chatting.message.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.message.api.port.ChatMessageService;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import com.idle.weather.chatting.message.repository.ChatMessageR2dbcRepository;
import com.idle.weather.chatting.message.repository.ReactiveChatMessageEntity;
import com.idle.weather.chatting.message.service.unit.ChatMessagePersistenceUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageR2dbcRepository chatMessageRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ChatMessagePersistenceUnit persistenceUnit;
    private final ObjectMapper objectMapper;

    private static final String MESSAGE_CACHE_PREFIX = "chatroom:";

    @Override
    public Mono<ChatMessageResponse> sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest chatMessageRequest) {
        return persistenceUnit.persist(chatRoomId, senderId, chatMessageRequest);
    }

    @Override
    public Flux<ChatMessageResponse> getRecentMessages(Long chatRoomId) {
        String cacheKey = MESSAGE_CACHE_PREFIX + chatRoomId + ":latest";
        Range<Long> range = Range.unbounded(); // 모든 메시지 조회

        return reactiveRedisTemplate.opsForZSet()
                .range(cacheKey, range)
                .flatMap(data -> {
                    try {
                        ReactiveChatMessageEntity chatMessage = objectMapper.readValue(data, ReactiveChatMessageEntity.class);
                        return Mono.just(ChatMessageResponse.from(chatMessage));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("getRecentMessages : 채팅 메시지 역직렬화 실패", e));
                    }
                });
    }

    @Override
    public Flux<ChatMessageResponse> getMessages(Long chatRoomId, int page, int size) {
        int offset = page * size;
        return chatMessageRepository.findByChatRoomId(chatRoomId)
                .skip(offset)
                .take(size)
                .map(ChatMessageResponse::from);
    }
}
