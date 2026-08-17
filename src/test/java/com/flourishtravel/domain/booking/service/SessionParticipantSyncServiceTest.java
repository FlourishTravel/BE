package com.flourishtravel.domain.booking.service;

import com.flourishtravel.domain.booking.entity.BookingGuest;
import com.flourishtravel.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionParticipantSyncServiceTest {

    @Test
    void fullPassengerListMatchingLeadName_skipsBookerOnly() {
        User lead = user("Nguyễn Văn A");
        List<BookingGuest> guests = List.of(
                guest("Nguyễn Văn A", 0),
                guest("Trần Thị B", 1),
                guest("Lê Văn C", 2),
                guest("Phạm Thị D", 3));

        var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 4);

        assertEquals(3, plan.companions().size());
        assertEquals("Trần Thị B", plan.companions().get(0).getFullName());
        assertEquals("Nguyễn Văn A", plan.skippedFormLead().getFullName());
    }

    @Test
    void fullPassengerListDifferentAccountName_dropsFormBooker() {
        User lead = user("Nguyen Van A");
        List<BookingGuest> guests = List.of(
                guest("Nguyễn Văn A", 0),
                guest("Trần Thị B", 1),
                guest("Lê Văn C", 2),
                guest("Phạm Thị D", 3));

        var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 4);

        assertEquals(3, plan.companions().size());
        assertEquals("Nguyễn Văn A", plan.skippedFormLead().getFullName());
        assertEquals(List.of("Trần Thị B", "Lê Văn C", "Phạm Thị D"),
                plan.companions().stream().map(BookingGuest::getFullName).toList());
    }

    @Test
    void companionsOnlySeedList_keepsAll() {
        User lead = user("Nguyễn Văn A");
        List<BookingGuest> guests = List.of(
                guest("Người đi cùng 1", 0),
                guest("Người đi cùng 2", 1),
                guest("Người đi cùng 3", 2));

        var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 4);

        assertEquals(3, plan.companions().size());
        assertNull(plan.skippedFormLead());
    }

    @Test
    void singleGuestBooking_storesOnlyLead() {
        User lead = user("Nguyễn Văn A");
        List<BookingGuest> guests = List.of(guest("Nguyễn Văn A", 0));

        var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 1);

        assertTrue(plan.companions().isEmpty());
        assertEquals("Nguyễn Văn A", plan.skippedFormLead().getFullName());
    }

    @Test
    void extraGuestsBeyondGuestCount_areCapped() {
        User lead = user("Nguyễn Văn A");
        List<BookingGuest> guests = List.of(
                guest("Nguyễn Văn A", 0),
                guest("Trần Thị B", 1),
                guest("Lê Văn C", 2),
                guest("Thừa 1", 3),
                guest("Thừa 2", 4));

        var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 3);

        assertEquals(2, plan.companions().size());
        assertEquals(List.of("Trần Thị B", "Lê Văn C"),
                plan.companions().stream().map(BookingGuest::getFullName).toList());
    }

    @Test
    void fiveBookingsOfFour_wouldHaveBeenTwentyFive_nowTwenty() {
        User lead = user("Account Name");
        int roster = 0;
        for (int i = 0; i < 5; i++) {
            List<BookingGuest> guests = List.of(
                    guest("Người đặt " + i, 0),
                    guest("Kèm A " + i, 1),
                    guest("Kèm B " + i, 2),
                    guest("Kèm C " + i, 3));
            var plan = SessionParticipantSyncService.selectCompanionsForRoster(lead, guests, 4);
            roster += 1 + plan.companions().size();
        }
        assertEquals(20, roster);
    }

    @Test
    void matchingNameIgnoresExtraWhitespace() {
        User lead = user("Nguyễn  Văn   A");
        BookingGuest g = guest("  Nguyễn Văn A ", 0);
        assertTrue(SessionParticipantSyncService.isBookingGuestDuplicateOfLead(g, lead));
    }

    @Test
    void sameNameDifferentDob_isNotDuplicate() {
        User lead = user("Nguyễn Văn A");
        lead.setDateOfBirth(LocalDate.of(1990, 1, 1));
        BookingGuest g = guest("Nguyễn Văn A", 0);
        g.setDateOfBirth(LocalDate.of(2015, 5, 5));
        assertTrue(!SessionParticipantSyncService.isBookingGuestDuplicateOfLead(g, lead));
    }

    private static User user(String fullName) {
        return User.builder().fullName(fullName).email("a@example.com").passwordHash("x").build();
    }

    private static BookingGuest guest(String fullName, int sortOrder) {
        return BookingGuest.builder().fullName(fullName).sortOrder(sortOrder).build();
    }
}
