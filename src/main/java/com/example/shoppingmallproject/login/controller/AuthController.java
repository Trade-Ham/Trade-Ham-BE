package com.example.shoppingmallproject.login.controller;

import com.example.shoppingmallproject.login.dto.TokenRefreshRequest;
import com.example.shoppingmallproject.login.dto.TokenResponseDto;
import com.example.shoppingmallproject.login.dto.UpdateUserInfoRequest;
import com.example.shoppingmallproject.login.security.JwtTokenProvider;
import com.example.shoppingmallproject.login.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/oauth2/authorization")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 로그인 URL로 리디렉션
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> redirectKakaoLogin() {
        String kakaoLoginUrl = authService.getKakaoLoginUrl();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(kakaoLoginUrl)).build();
    }

    /**
     * 사용자 정보 업데이트 (Access Token 사용)
     */
    @PatchMapping("/update")
    public ResponseEntity<String> updateUserInfo(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody UpdateUserInfoRequest request) {

        try {
            // "Bearer " 제거하고 토큰만 전달
            accessToken = accessToken.replace("Bearer ", "");
            authService.updateUserInfo(accessToken, request);
            return ResponseEntity.ok("정보 업데이트 성공");
        } catch (Exception e) {
            log.error("정보 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body("정보 업데이트 실패");
        }
    }

    /**
     * Refresh Token을 사용하여 JWT Access Token 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refreshToken(HttpServletResponse response, @RequestBody TokenRefreshRequest request) {
        try {
            TokenResponseDto tokenResponse = authService.refreshJwtTokens(response, request.getRefreshToken());
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
