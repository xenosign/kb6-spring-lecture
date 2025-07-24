package org.example.kb6spring.repository.seat;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.*;
import org.example.kb6spring.domain.seat.SeatVo;
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
    List<SeatVo> findAllSeats() {
        return seatMapper.findAllSeats();
    };

    // 특정 구역의 좌석 조회
    List<SeatVo> findSeatsBySection(String section) {
        return seatMapper.findSeatsBySection(section);
    };

    // 특정 좌석 조회
    SeatVo findSeatById(Long seatId) {
        return seatMapper.findSeatById(seatId);
    };

    // 좌석 추가
    int insertSeat(SeatVo seat) {
        return seatMapper.insertSeat(seat);
    };

    // 좌석 정보 수정
    int updateSeat(SeatVo seat) {
        return seatMapper.updateSeat(seat);
    };

    // 좌석 삭제
    int deleteSeat(Long seatId) {
        return seatMapper.deleteSeat(seatId);
    };
}
