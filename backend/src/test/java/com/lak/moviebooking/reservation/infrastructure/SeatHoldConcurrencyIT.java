package com.lak.moviebooking.reservation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.reservation.application.SeatHoldCommand;
import com.lak.moviebooking.reservation.application.SeatHoldManagement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SeatHoldConcurrencyIT extends SeatHoldManagementIT {

    @Autowired
    private SeatHoldManagement seatHoldManagement;

    @Test
    void onlyOneConcurrentRequestCanHoldTheSameSeat() throws Exception {
        Fixture fixture = fixture();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(request(fixture.firstUserId(), fixture.showtimeId(), fixture.standardSeatId(), "first", ready, start));
            Future<Boolean> second = executor.submit(request(fixture.secondUserId(), fixture.showtimeId(), fixture.standardSeatId(), "second", ready, start));
            ready.await();
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        }
    }

    private Callable<Boolean> request(java.util.UUID userId, java.util.UUID showtimeId, java.util.UUID seatId,
                                      String key, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            try {
                seatHoldManagement.create(userId, new SeatHoldCommand(showtimeId, List.of(seatId), key));
                return true;
            }
            catch (ApplicationException exception) {
                return false;
            }
        };
    }
}
