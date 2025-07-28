package org.example.kb6spring.service.seat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.seat.SeatReservationVo;
import org.example.kb6spring.repository.seat.SeatReservationRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatReservationService {
    private final SeatReservationRepository seatReservationRepository;

    public List<SeatReservationVo> findAllReservations() {
        return seatReservationRepository.findAllReservations();
    }

    // 특정 좌석 예약 조회
    public SeatReservationVo findBySeatId(Long seatId) {
        return seatReservationRepository.findBySeatId(seatId);
    }

    // 특정 사용자 예약 전체 조회
    public List<SeatReservationVo> findByUserId(Long userId) {
        return seatReservationRepository.findByUserId(userId);
    }

    // PENDING 예약 시도
    public int makePending(SeatReservationVo reservation) {
        SeatReservationVo existing = findBySeatId(reservation.getSeatId());


        if (existing == null) {
            // 최초 예약: INSERT
            return seatReservationRepository.insertReservation(reservation);
        } else if ("AVAILABLE".equals(existing.getStatus())) {
            reservation.setStatus("PENDING");
            return seatReservationRepository.updateReservationStatus(reservation);
        } else {
            // RESERVED나 PENDING 등 다른 상태인 경우 예외 처리
            throw new IllegalStateException("이미 예약되었거나 보류 중인 좌석입니다.");
        }
    }

    // PENDING → CANCELLED로 변경
    public int cancelPending(Long seatId, Long userId) {
        SeatReservationVo existing = findBySeatId(seatId);

        if (existing != null && "PENDING".equals(existing.getStatus()) && userId.equals(existing.getUserId())) {
            existing.setStatus("CANCELLED");
            return seatReservationRepository.updateReservationStatus(existing);
        } else {
            throw new IllegalStateException("취소할 수 있는 보류 중인 예약이 아닙니다.");
        }
    }

    // 예약 확정 (PENDING → RESERVED)
    public int confirmReservation(SeatReservationVo reservation) {
        SeatReservationVo existing = findBySeatId(reservation.getSeatId());

        if ("PENDING".equals(existing.getStatus())) {
            log.info("##################### {}", reservation);
            reservation.setStatus("RESERVED");
            return seatReservationRepository.updateReservationStatus(reservation);
        } else {
            throw new IllegalStateException("보류 상태가 아니거나 사용자 정보가 일치하지 않습니다.");
        }
    }

    // 예약 상태 수정 (예: 수동 상태 변경)
    public int updateReservationStatus(SeatReservationVo reservation) {
        return seatReservationRepository.updateReservationStatus(reservation);
    }

    // 예약 삭제
    public int deleteReservation(Long reservationId) {
        return seatReservationRepository.deleteReservation(reservationId);
    }
    
    // 스케쥴러 추가
}

