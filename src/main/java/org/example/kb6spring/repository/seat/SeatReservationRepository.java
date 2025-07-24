package org.example.kb6spring.repository.seat;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.*;
import org.example.kb6spring.domain.seat.SeatReservationVo;
import org.example.kb6spring.mapper.seat.SeatReservationMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SeatReservationRepository {
    private final SeatReservationMapper seatReservationMapper;

    List<SeatReservationVo> findAllReservations() {
        return seatReservationMapper.findAllReservations();
    };

    // 특정 좌석 예약 조회
    SeatReservationVo findBySeatId(Long seatId) {
        return seatReservationMapper.findBySeatId(seatId);
    };

    // 특정 사용자 예약 전체 조회
    List<SeatReservationVo> findByUserId(Long userId) {
        return seatReservationMapper.findByUserId(userId);
    };

    // 예약 생성
    int insertReservation(SeatReservationVo reservation) {
        return seatReservationMapper.insertReservation(reservation);
    };

    // 예약 상태 수정 (예: 취소로 변경)
    int updateReservationStatus(SeatReservationVo reservation) {
        return seatReservationMapper.updateReservationStatus(reservation);
    };

    // 예약 삭제
    int deleteReservation(Long reservationId) {
        return seatReservationMapper.deleteReservation(reservationId);
    };
}
