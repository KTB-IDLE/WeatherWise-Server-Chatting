package com.idle.weather.chatting.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.chatroom.repository.ChatRoomR2dbcRepository;
import com.idle.weather.chatting.message.api.port.ChatMessageService;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import com.idle.weather.chatting.message.repository.ChatMessageR2dbcRepository;
import com.idle.weather.chatting.message.repository.ReactiveChatMessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageR2dbcRepository chatMessageRepository;
    private final ChatRoomR2dbcRepository chatRoomRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String MESSAGE_CACHE_PREFIX = "chatroom:";
    private static final int MAX_CACHE_SIZE = 100;

    @Override
    public Mono<ChatMessageResponse> sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest chatMessageRequest) {
        return chatRoomRepository.existsById(chatRoomId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("해당하는 ChatRoom을 찾을 수 없습니다."));
                    }
                    ReactiveChatMessageEntity chatMessage = ReactiveChatMessageEntity.createChatMessage(chatRoomId, senderId, chatMessageRequest.message());
                    return chatMessageRepository.save(chatMessage);
                })
                .flatMap(saved -> cacheMessageInRedis(chatRoomId, saved)
                        .then(Mono.just(ChatMessageResponse.from(saved))));
    }

    private Mono<Void> cacheMessageInRedis(Long chatRoomId, ReactiveChatMessageEntity chatMessage) {
        String cacheKey = MESSAGE_CACHE_PREFIX + chatRoomId + ":latest";
        try {
            String serializedMessage = objectMapper.writeValueAsString(chatMessage);
            double score = chatMessage.getTimestamp().atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();
            ReactiveZSetOperations<String, String> zSetOps = reactiveRedisTemplate.opsForZSet();

            return zSetOps.add(cacheKey, serializedMessage, score)
                    .flatMap(success -> {
                        if (success) {
                            // ZSet의 현재 크기 조회
                            return zSetOps.size(cacheKey)
                                    .flatMap(size -> {
                                        long removeCount = size - MAX_CACHE_SIZE;
                                        if (removeCount > 0) {
                                            // 오래된 메시지 제거
                                            Range<Long> range = Range.closed(0L, removeCount -1);
                                            return zSetOps.removeRange(cacheKey, range).then();
                                        } else {
                                            return Mono.empty();
                                        }
                                    });
                        } else {
                            return Mono.empty();
                        }
                    });
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("cacheMessageInRedis : 채팅 메시지 직렬화 실패", e));
        }
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
