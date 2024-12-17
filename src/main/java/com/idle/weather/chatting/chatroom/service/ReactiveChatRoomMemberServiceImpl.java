package com.idle.weather.chatting.chatroom.service;

import com.idle.weather.chatting.chatroom.api.port.ChatRoomMemberService;
import com.idle.weather.chatting.chatroom.api.response.ChatRoomMemberResponse;
import com.idle.weather.chatting.chatroom.repository.ChatRoomMemberR2dbcRepository;
import com.idle.weather.chatting.chatroom.repository.ChatRoomR2dbcRepository;
import com.idle.weather.chatting.chatroom.repository.ReactiveChatRoomMemberEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveChatRoomMemberServiceImpl implements ChatRoomMemberService {

    private final ChatRoomMemberR2dbcRepository chatRoomMemberRepository;
    private final ChatRoomR2dbcRepository chatRoomRepository;

    @Override
    public Mono<ChatRoomMemberResponse> joinChatRoom(Long chatRoomId, Long userId) {
        return chatRoomRepository.findById(chatRoomId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("ChatRoom not found with id :" + chatRoomId)))
                .flatMap(chaRoom -> {
                    ReactiveChatRoomMemberEntity chatRoomMember = ReactiveChatRoomMemberEntity.createChatRoomMember(chatRoomId, userId);
                    return chatRoomMemberRepository.save(chatRoomMember);
                })
                .map(ChatRoomMemberResponse::from);
    }

    @Override
    public Mono<Void> leaveChatRoom(Long chatRoomId, Long userId) {
        return chatRoomMemberRepository.existsByChatRoomIdAndUserId(chatRoomId, userId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("User is not a member of the chatRoom"));
                    }
                    return chatRoomMemberRepository.deleteByChatRoomIdAndUserId(chatRoomId, userId);
                });
    }

    @Override
    public Flux<Long> getChatRoomUsers(Long chatRoomId) {
        return chatRoomMemberRepository.findUserIdsByChatRoomId(chatRoomId);
    }
}
