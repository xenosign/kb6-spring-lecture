package org.example.kb6spring.dto.seat;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SeatDto {
    private Long seatId;
    private String section;
    private int seatRow;
    private int seatNumber;

    private Long reservationId;
    private Long userId;
    private String status;
    private Timestamp reservedAt;
}
