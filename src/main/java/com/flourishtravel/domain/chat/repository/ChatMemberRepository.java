package com.flourishtravel.domain.chat.repository;

import com.flourishtravel.domain.chat.entity.ChatMember;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMember.ChatMemberId> {

    boolean existsByRoomAndUser(ChatRoom room, User user);

    List<ChatMember> findByRoom(ChatRoom room);
}
