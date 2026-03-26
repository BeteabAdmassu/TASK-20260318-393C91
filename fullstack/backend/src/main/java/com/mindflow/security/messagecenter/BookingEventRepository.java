package com.mindflow.security.messagecenter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BookingEventRepository extends JpaRepository<BookingEventEntity, Long> {
    List<BookingEventEntity> findByReservationSuccessSentFalse();

    List<BookingEventEntity> findByReservationSuccessSentFalseAndTenantId(String tenantId);

    List<BookingEventEntity> findByArrivalReminderSentFalseAndStartTimeBefore(Instant time);

    List<BookingEventEntity> findByArrivalReminderSentFalseAndStartTimeBeforeAndTenantId(Instant time, String tenantId);

    List<BookingEventEntity> findByMissedCheckInSentFalseAndStartTimeLessThanEqual(Instant time);

    List<BookingEventEntity> findByMissedCheckInSentFalseAndStartTimeLessThanEqualAndTenantId(Instant time, String tenantId);
}
