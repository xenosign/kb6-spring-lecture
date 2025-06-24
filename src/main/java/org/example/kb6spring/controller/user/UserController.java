package org.example.kb6spring.controller.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.user.User;
import org.example.kb6spring.dto.user.LoginRequestDto;
import org.example.kb6spring.security.service.CustomUserDetailsService;
import org.example.kb6spring.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/register")
    public String register(Model model) {
        return "/user/register";
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        System.out.println(user.getUsername());
        System.out.println(user.getPassword());
        System.out.println(user.getRole());
        userService.save(user);
        return "redirect:/user/login";
    }

    @GetMapping("login-success")
    public String loginSucceed(Model model, Principal principal, Authentication auth) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(principal.getName());

        log.info("===========> userDetails: {}", userDetails);
        log.info("===========> principal: {}", principal);
        log.info("===========> auth: {}", auth);

        model.addAttribute("user", userDetails.getUsername());
        model.addAttribute("auth", userDetails.getAuthorities());
        return "user/login-success";
    }

    @GetMapping("login-failure")
    public String loginFailed(Model model) {
        return "user/login-failure";
    }
}
