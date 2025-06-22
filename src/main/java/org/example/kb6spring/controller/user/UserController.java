package org.example.kb6spring.controller.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.user.User;
import org.example.kb6spring.dto.user.LoginRequestDto;
import org.example.kb6spring.security.service.CustomUserDetailsService;
import org.example.kb6spring.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;

    @GetMapping("/login")
    public String login(Model model) {
        return "user/login";
    }

    @GetMapping("login-success")
    public String loginSucceed(Model model, Principal principal) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(principal.getName());
        model.addAttribute("user", userDetails);
        return "user/login-success";
    }

    @GetMapping("login-failure")
    public String loginFailed(Model model) {
        return "user/login-failure";
    }

    @GetMapping("/register")
    public String register(Model model) {
        return "/user/register";
    }

    @PostMapping("/register")
    public String register(User user, Model model) {
        userService.save(user);
        return "redirect:/user/login";
    }
}
