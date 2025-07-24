package org.example.kb6spring.domain.seat;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SeatReservationVo {
    private Long reservationId;
    private Long seatId;
    private String userId;
    private Timestamp reservationTime;
    private String status;
    private Timestamp updatedAt;
}
