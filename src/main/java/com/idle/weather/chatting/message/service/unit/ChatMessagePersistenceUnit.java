package com.idle.weather.chatting.message.service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idle.weather.chatting.message.api.request.ChatMessageRequest;
import com.idle.weather.chatting.message.api.response.ChatMessageResponse;
import com.idle.weather.chatting.message.repository.ChatMessageR2dbcRepository;
import com.idle.weather.chatting.message.repository.ReactiveChatMessageEntity;
import com.idle.weather.chatting.chatroom.repository.ChatRoomR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessagePersistenceUnit {

    private final ChatMessageR2dbcRepository chatMessageRepository;
    private final ChatRoomR2dbcRepository chatRoomRepository;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    private static final String MESSAGE_CACHE_PREFIX = "chatroom:";
    private static final int MAX_CACHE_SIZE = 100;

    public Mono<ChatMessageResponse> persist(Long chatRoomId, Long senderId, ChatMessageRequest chatMessageRequest) {
        return transactionalOperator.transactional(
                chatRoomRepository.findById(chatRoomId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("해당하는 채팅방을 찾을 수 없습니다.")))
                        .flatMap(chatRoom -> {
                            ReactiveChatMessageEntity chatMessage = ReactiveChatMessageEntity.createChatMessage(chatRoomId, senderId, chatMessageRequest.message());

                            return chatMessageRepository.save(chatMessage)
                                    .flatMap(saved -> cacheMessage(chatRoomId, saved)
                                            .onErrorResume(e -> {
                                                log.warn("Redis 캐싱 실패: chatRoomId={}, senderId={}, message={}, 원인={}",
                                                        chatRoomId, senderId, chatMessageRequest.message(), e.getMessage(), e);
                                                return Mono.empty();
                                            })
                                            .thenReturn(ChatMessageResponse.from(saved))
                                    );
                        })
        );
    }

    private Mono<Void> cacheMessage(Long chatRoomId, ReactiveChatMessageEntity chatMessage) {
        String cacheKey = MESSAGE_CACHE_PREFIX + chatRoomId + ":latest";
        try {
            String serializedMessage = objectMapper.writeValueAsString(chatMessage);
            double score = chatMessage.getTimestamp().atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();
            ReactiveZSetOperations<String, String> zSetOps = reactiveRedisTemplate.opsForZSet();

            return zSetOps.add(cacheKey, serializedMessage, score)
                    .flatMap(success -> {
                        if (success) {
                            return zSetOps.size(cacheKey)
                                    .flatMap(size -> {
                                        long removeCount = size - MAX_CACHE_SIZE;
                                        if (removeCount > 0) {
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
            return Mono.error(new RuntimeException("Redis 직렬화 실패", e));
        }
    }
}
