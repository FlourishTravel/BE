package com.flourishtravel.domain.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloraGroupChatTriggerTest {

    @Test
    void repliesWhenMentioned() {
        assertTrue(FloraGroupChatTrigger.shouldReply("@Flora mấy giờ tập trung?"));
        assertTrue(FloraGroupChatTrigger.shouldReply("Flora ơi mưa thì tour có đi không"));
        assertTrue(FloraGroupChatTrigger.shouldReply("/flora chính sách hủy"));
        assertTrue(FloraGroupChatTrigger.shouldReply("hỏi Flora lịch trình ngày mai"));
    }

    @Test
    void repliesOnExistingTopicsWithoutMention() {
        assertTrue(FloraGroupChatTrigger.shouldReply("Hủy tour được hoàn tiền không?"));
        assertTrue(FloraGroupChatTrigger.shouldReply("Mấy giờ tập trung vậy?"));
        assertTrue(FloraGroupChatTrigger.shouldReply("Bão thì tour còn khởi hành không"));
        assertTrue(FloraGroupChatTrigger.shouldReply("Bị ngộ độc thì làm sao?"));
    }

    @Test
    void ignoresSmallTalk() {
        assertFalse(FloraGroupChatTrigger.shouldReply("ok"));
        assertFalse(FloraGroupChatTrigger.shouldReply("cảm ơn"));
        assertFalse(FloraGroupChatTrigger.shouldReply("haha"));
        assertFalse(FloraGroupChatTrigger.shouldReply("Mọi người 7h có mặt sảnh nha"));
        assertFalse(FloraGroupChatTrigger.shouldReply(""));
        assertFalse(FloraGroupChatTrigger.shouldReply(null));
    }

    @Test
    void stripsMentionForTheChatbot() {
        assertEquals("mấy giờ tập trung?", FloraGroupChatTrigger.stripMention("@Flora mấy giờ tập trung?"));
        assertEquals("chính sách hủy", FloraGroupChatTrigger.stripMention("/flora chính sách hủy"));
    }
}
