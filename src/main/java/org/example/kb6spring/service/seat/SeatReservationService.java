package org.example.kb6spring.service.seat;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.domain.seat.SeatReservationVo;
import org.example.kb6spring.repository.seat.SeatReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatReservationService {
    private final SeatReservationRepository seatReservationRepository;

    List<SeatReservationVo> findAllReservations() {
        return seatReservationRepository.findAllReservations();
    };

    // 특정 좌석 예약 조회
    SeatReservationVo findBySeatId(Long seatId) {
        return seatReservationRepository.findBySeatId(seatId);
    };

    // 특정 사용자 예약 전체 조회
    List<SeatReservationVo> findByUserId(Long userId) {
        return seatReservationRepository.findByUserId(userId);
    };

    // 예약 생성
    int insertReservation(SeatReservationVo reservation) {
        return seatReservationRepository.insertReservation(reservation);
    };

    // 예약 상태 수정 (예: 취소로 변경)
    int updateReservationStatus(SeatReservationVo reservation) {
        return seatReservationRepository.updateReservationStatus(reservation);
    };

    // 예약 삭제
    int deleteReservation(Long reservationId) {
        return seatReservationRepository.deleteReservation(reservationId);
    };
}
