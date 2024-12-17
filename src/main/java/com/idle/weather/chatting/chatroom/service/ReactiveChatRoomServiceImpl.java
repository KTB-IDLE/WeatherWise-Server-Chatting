package com.idle.weather.chatting.chatroom.service;

import com.idle.weather.chatting.chatroom.api.port.ChatRoomService;
import com.idle.weather.chatting.chatroom.api.response.ChatRoomResponse;
import com.idle.weather.chatting.chatroom.repository.ChatRoomR2dbcRepository;
import com.idle.weather.chatting.chatroom.repository.ReactiveChatRoomEntity;
import com.idle.weather.chatting.weatheralert.repository.WeatherAlertR2dbcRepository;
import com.idle.weather.chatting.weatheralert.repository.ReactiveWeatherAlertEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomR2dbcRepository chatRoomRepository;
    private final WeatherAlertR2dbcRepository weatherAlertRepository;
    private final ReactiveTransactionManager transactionManager;

    @Override
    public Mono<ReactiveChatRoomEntity> getOrCreateChatRoom(String parentRegionCode, String parentRegionName) {
        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);

        return chatRoomRepository.findByParentRegionCode(parentRegionCode)
                .switchIfEmpty(
                        Mono.defer(() -> {
                            ReactiveChatRoomEntity newChatRoom = ReactiveChatRoomEntity.createChatRoom(parentRegionCode, parentRegionName);
                            return chatRoomRepository.save(newChatRoom)
                                    .onErrorResume(DuplicateKeyException.class, e -> {
                                        log.warn("중복 키 발생: {}, 기존 채팅방 반환", parentRegionCode);
                                        return chatRoomRepository.findByParentRegionCode(parentRegionCode);
                                    });
                        })
                )
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> deleteChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("ChatRoom not found with id: " + chatRoomId)))
                .flatMap(chatRoomRepository::delete);
    }

    @Override
    public Mono<Void> saveChatRoom(ReactiveChatRoomEntity chatRoom) {
        return chatRoomRepository.save(chatRoom).then();
    }

    @Override
    public Flux<ChatRoomResponse> getAllChatRooms() {
        return chatRoomRepository.findAll()
                .map(ChatRoomResponse::from);
    }

    @Override
    public Flux<ChatRoomResponse> getAllActivatedChatRooms() {
        return chatRoomRepository.findAllActivatedChatRooms()
                .map(ChatRoomResponse::from);
    }

    @Override
    public Mono<Void> deleteOldDeactivatedChatRooms(int day) {
        return chatRoomRepository.findAllDeactivatedOlderThan(LocalDateTime.now().minusDays(day))
                .flatMap(chatRoom -> chatRoomRepository.delete(chatRoom)
                        .doOnSuccess(unused -> log.info("오래된 비활성화 채팅방 삭제: {}", chatRoom.getName())))
                .then();
    }

    @Override
    public Mono<ReactiveChatRoomEntity> getChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("ChatRoom not found with id : " + chatRoomId)));
    }

    @Override
    public Mono<Void> updateChatRoomName(Long chatRoomId) {
        return weatherAlertRepository.findByChatRoomIdAndIsActivatedTrue(chatRoomId)
                .map(ReactiveWeatherAlertEntity::getAlertType)
                .collect(Collectors.toSet())
                .flatMap(alertTypes -> {
                    return getChatRoomById(chatRoomId)
                            .flatMap(chatRoom -> {
                                String newName = chatRoom.getParentRegionName() + " " +
                                        String.join(", ", alertTypes) + " 채팅방";
                                chatRoom.updateName(newName);
                                return saveChatRoom(chatRoom);
                            });
                });

    }
}
