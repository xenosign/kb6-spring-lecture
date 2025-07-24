package org.example.kb6spring.service.seat;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.domain.seat.SeatVo;
import org.example.kb6spring.mapper.seat.SeatMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final SeatMapper seatMapper;

    public void createSeats() {
        seatMapper.deleteAllSeat();

        char[] sections = {'A', 'B', 'C', 'D', 'E', 'F'};

        for (char section : sections) {
            for (int i = 1; i <= 20; i++) {
                for (int j = 1; j <= 20; j++) {
                    SeatVo seat = new SeatVo();
                    seat.setSection(String.valueOf(section));
                    seat.setSeatRow(i);
                    seat.setSeatNumber(j);
                    seatMapper.insertSeat(seat);
                }
            }
        }
    }
}
