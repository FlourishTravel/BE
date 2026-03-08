package com.flourishtravel.domain.chat.entity;

import com.flourishtravel.common.entity.BaseEntity;
import com.flourishtravel.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_reactions", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "reaction_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reaction_type", nullable = false, length = 30)
    private String reactionType;
}
