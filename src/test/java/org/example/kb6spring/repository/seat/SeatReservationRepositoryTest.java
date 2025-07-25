package org.example.kb6spring.repository.seat;

import org.example.kb6spring.config.JpaConfig;
import org.example.kb6spring.config.RootConfig;
import org.example.kb6spring.config.TestConfig;
import org.example.kb6spring.domain.seat.SeatReservationVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class, TestConfig.class })
public class SeatReservationRepositoryTest {

    @Autowired
    private SeatReservationRepository reservationRepository;

    @Test
    public void insertReservation() {
        SeatReservationVo reservation = new SeatReservationVo();
        reservation.setSeatId(1L);
        reservation.setUserId(100L);
        reservation.setStatus("RESERVED");

        int result = reservationRepository.insertReservation(reservation);
        assertEquals(1, result);
        assertNotNull(reservation.getReservationId());
    }

    @Test
    public void findAllReservations() {
        List<SeatReservationVo> list = reservationRepository.findAllReservations();
        assertNotNull(list);
    }

    @Test
    public void findBySeatId() {
        Long seatId = 1L;
        SeatReservationVo reservation = reservationRepository.findBySeatId(seatId);
        if (reservation != null) {
            assertEquals(seatId, reservation.getSeatId());
        }
    }

    @Test
    public void findByUserId() {
        Long userId = 100L;
        List<SeatReservationVo> reservations = reservationRepository.findByUserId(userId);
        assertNotNull(reservations);
        for (SeatReservationVo res : reservations) {
            assertEquals(userId, res.getUserId());
        }
    }

    @Test
    public void updateReservationStatus() {
        SeatReservationVo reservation = new SeatReservationVo();
        reservation.setSeatId(2L);
        reservation.setUserId(200L);
        reservation.setStatus("RESERVED");
        reservationRepository.insertReservation(reservation);

        reservation.setStatus("CANCELLED");
        int updated = reservationRepository.updateReservationStatus(reservation);
        assertEquals(1, updated);
    }

    @Test
    public void deleteReservation() {
        SeatReservationVo reservation = new SeatReservationVo();
        reservation.setSeatId(3L);
        reservation.setUserId(300L);
        reservation.setStatus("RESERVED");
        reservationRepository.insertReservation(reservation);

        Long id = reservation.getReservationId();
        int deleted = reservationRepository.deleteReservation(id);
        assertEquals(1, deleted);

        SeatReservationVo afterDelete = reservationRepository.findBySeatId(3L);
        assertNull(afterDelete);
    }
}
