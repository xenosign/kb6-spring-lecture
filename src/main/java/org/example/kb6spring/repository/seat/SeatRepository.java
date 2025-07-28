package org.example.kb6spring.repository.seat;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.*;
import org.example.kb6spring.domain.seat.SeatVo;
import org.example.kb6spring.dto.seat.SeatDto;
import org.example.kb6spring.mapper.seat.SeatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SeatRepository {
    @Autowired
    private SeatMapper seatMapper;

    // 모든 좌석 조회
    public List<SeatDto> findAllSeatsWithReservation() {
        return seatMapper.findAllSeatsWithReservation();
    };

    // 특정 구역의 좌석 조회
    public List<SeatVo> findSeatsBySection(String section) {
        return seatMapper.findSeatsBySection(section);
    };

    // 특정 좌석 조회
    public SeatVo findSeatById(Long seatId) {
        return seatMapper.findSeatById(seatId);
    };

    // 좌석 추가
    public int insertSeat(SeatVo seat) {
        return seatMapper.insertSeat(seat);
    };

    // 좌석 정보 수정
    public int updateSeat(SeatVo seat) {
        return seatMapper.updateSeat(seat);
    };

    // 좌석 삭제
    public int deleteSeat(Long seatId) {
        return seatMapper.deleteSeat(seatId);
    };

    public int deleteAllSeat() {
        return seatMapper.deleteAllSeat();
    }
}
