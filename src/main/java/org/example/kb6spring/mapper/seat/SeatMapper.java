package org.example.kb6spring.mapper.seat;

import org.apache.ibatis.annotations.*;
import org.example.kb6spring.domain.seat.SeatVo;
import org.example.kb6spring.dto.seat.SeatDto;

import java.util.List;

@Mapper
public interface SeatMapper {
    // 모든 좌석 조회
    @Select(
            "SELECT " +
                    "  s.seat_id AS seatId, " +
                    "  s.section AS section, " +
                    "  s.seat_row AS seatRow, " +
                    "  s.seat_number AS seatNumber, " +
                    "  r.reservation_id AS reservationId, " +
                    "  r.user_id AS userId, " +
                    "  r.status AS status, " +
                    "  r.reserved_at AS reservedAt " +
                    "FROM seats s " +
                    "LEFT JOIN reservations r " +
                    "  ON s.seat_id = r.seat_id "+
                    "ORDER BY s.section, s.seat_row, s.seat_number"
    )
    List<SeatDto> findAllSeatsWithReservation();


    // 특정 구역의 좌석 조회
    @Select("SELECT * FROM seats WHERE section = #{section} ORDER BY seat_row, seat_number")
    List<SeatVo> findSeatsBySection(String section);

    // 특정 좌석 조회
    @Select("SELECT * FROM seats WHERE seat_id = #{seatId}")
    SeatVo findSeatById(Long seatId);

    // 좌석 추가
    @Insert("INSERT INTO seats (section, seat_row, seat_number) " +
            "VALUES (#{section}, #{seatRow}, #{seatNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "seatId")
    int insertSeat(SeatVo seat);

    // 좌석 정보 수정
    @Update("UPDATE seats SET section = #{section}, seat_row = #{seatRow}, seat_number = #{seatNumber} " +
            "WHERE seat_id = #{seatId}")
    int updateSeat(SeatVo seat);

    // 좌석 삭제
    @Delete("DELETE FROM seats WHERE seat_id = #{seatId}")
    int deleteSeat(Long seatId);

    // 좌석 전체 삭제
    @Delete("DELETE FROM seats")
    int deleteAllSeat();
}