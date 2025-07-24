package org.example.kb6spring.controller.seat;


import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.service.seat.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Seat 컨트롤러")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {
    private final SeatService seatService;

    @GetMapping("/create")
    public void createSeat() {
        seatService.createSeats();
    }
}
