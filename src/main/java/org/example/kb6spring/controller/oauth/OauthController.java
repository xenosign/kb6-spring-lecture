package org.example.kb6spring.controller.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.oauth.KakaoUserInfoDto;
import org.example.kb6spring.service.oauth.KakaoOauthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/oauth")
public class OauthController {
    private final KakaoOauthService kakaoOauthService;

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        KakaoUserInfoDto userInfo = kakaoOauthService.processKakaoLogin(code);

        Cookie cookie = new Cookie("jwt", userInfo.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60); // 1시간

        response.addCookie(cookie);

        String frontendRedirect = (state != null) ? state : "http://localhost:5173";

        String encodedEmail = URLEncoder.encode(userInfo.getEmail(), "UTF-8");
        String encodedNickname = URLEncoder.encode(userInfo.getNickname(), "UTF-8");
        String encodedProfileImageUrl = URLEncoder.encode(userInfo.getProfileImageUrl(), "UTF-8");

        String redirectUrl = String.format(
                "%s?email=%s&nickname=%s&profileImage=%s",
                frontendRedirect,
                encodedEmail,
                encodedNickname,
                encodedProfileImageUrl
        );

        // 5. 리다이렉트
        response.sendRedirect(redirectUrl);
    }
}
