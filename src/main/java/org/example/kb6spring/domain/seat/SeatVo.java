package org.example.kb6spring.domain.seat;

import lombok.Data;

@Data
public class SeatVo {
    private Long seatId;      // seats.seat_id
    private String section;   // 구역명
    private int seatRow;      // 행 번호
    private int seatNumber;   // 열 번호
}