package org.example.kb6spring.dto.seat;

import lombok.Data;

@Data
public class SeatDto {
    private Long seatId;
    private String section;
    private int seatRow;
    private int seatNumber;

    private boolean reserved;
    private Long userId;
}
