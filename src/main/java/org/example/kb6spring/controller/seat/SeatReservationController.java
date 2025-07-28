package org.example.kb6spring.controller.seat;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.seat.SeatReservationVo;
import org.example.kb6spring.service.seat.SeatReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

@Api(tags = "SeatReservation 컨트롤러")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class SeatReservationController {
    private final SeatReservationService seatReservationService;
    private final SimpMessageSendingOperations messagingTemplate;

    @PostMapping("/pending")
    public ResponseEntity<SeatReservationVo> makePending(@RequestBody SeatReservationVo reservation) {
        seatReservationService.makePending(reservation);

        messagingTemplate.convertAndSend("/topic/seats/update", reservation);

        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/reserve")
    public ResponseEntity<SeatReservationVo> makeReservation(@RequestBody SeatReservationVo reservation) {
        log.info("@@@@@@@@@@@@@ {}", reservation);

        seatReservationService.confirmReservation(reservation);

        messagingTemplate.convertAndSend("/topic/seats/update", reservation);

        return ResponseEntity.ok(reservation);
    }

    @PutMapping("/update")
    public void updateReservation(@RequestBody SeatReservationVo reservation) {
        messagingTemplate.convertAndSend("/topic/seats/update", reservation);

        seatReservationService.updateReservationStatus(reservation);
    }

}
