package org.example.kb6spring.mapper.seat;

import org.apache.ibatis.annotations.*;
import org.example.kb6spring.domain.seat.SeatReservationVo;

import java.util.List;

@Mapper
public interface SeatReservationMapper {
    // 모든 예약 조회
    @Select("SELECT * FROM reservations ORDER BY reserved_at DESC")
    List<SeatReservationVo> findAllReservations();

    // 특정 좌석 예약 조회
    @Select("SELECT * FROM reservations WHERE seat_id = #{seatId}")
    SeatReservationVo findBySeatId(Long seatId);

    // 특정 사용자 예약 전체 조회
    @Select("SELECT * FROM reservations WHERE user_id = #{userId} ORDER BY reserved_at DESC")
    List<SeatReservationVo> findByUserId(Long userId);

    // 예약 생성
    @Insert("INSERT INTO reservations (seat_id, user_id, status) " +
            "VALUES (#{seatId}, #{userId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "reservationId")
    int insertReservation(SeatReservationVo reservation);

    // 예약 상태 수정 (예: 취소로 변경)
    @Update("UPDATE reservations SET status = #{status} " +
            "WHERE reservation_id = #{reservationId}")
    int updateReservationStatus(SeatReservationVo reservation);

    // 예약 삭제
    @Delete("DELETE FROM reservations WHERE reservation_id = #{reservationId}")
    int deleteReservation(Long reservationId);
}
