package org.example.kb6spring.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.user.User;
import org.example.kb6spring.exception.user.InvalidPasswordException;
import org.example.kb6spring.exception.user.UserNotFoundException;
import org.example.kb6spring.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public User findByUsername(String username) {
        log.debug("사용자 조회 요청: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("해당 user 를 찾을 수 없습니다: " + username));
    }

    @Transactional(readOnly = true)
    public User login(String username, String rawPassword) {
        User user = findByUsername(username);

        if (!user.getPassword().equals(rawPassword)) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
