CREATE TABLE seats (
                       seat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       section VARCHAR(50) NOT NULL,
                       seat_row INT NOT NULL,
                       seat_number INT NOT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uq_seat (section, seat_row, seat_number)
);

CREATE TABLE reservations (
                              reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              seat_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              reserved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              status ENUM('RESERVED', 'CANCELLED') DEFAULT 'RESERVED',
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              UNIQUE KEY uq_seat_reservation (seat_id),
                              INDEX idx_user (user_id),
                              FOREIGN KEY (seat_id) REFERENCES seats(seat_id)
);