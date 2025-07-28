package org.example.kb6spring.service.seat;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.domain.seat.SeatVo;
import org.example.kb6spring.dto.seat.SeatDto;
import org.example.kb6spring.mapper.seat.SeatMapper;
import org.example.kb6spring.repository.seat.SeatRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;

    public List<SeatDto> findAllSeatsWithReservation() {
        return seatRepository.findAllSeatsWithReservation();
    }

    public void createSeats() {
        seatRepository.deleteAllSeat();

        char[] sections = {'A', 'B', 'C', 'D', 'E', 'F'};

        for (char section : sections) {
            for (int i = 1; i <= 20; i++) {
                for (int j = 1; j <= 20; j++) {
                    SeatVo seat = new SeatVo();
                    seat.setSection(String.valueOf(section));
                    seat.setSeatRow(i);
                    seat.setSeatNumber(j);
                    seatRepository.insertSeat(seat);
                }
            }
        }
    }
}
