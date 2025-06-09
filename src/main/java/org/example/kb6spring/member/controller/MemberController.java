package org.example.kb6spring.member.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class MemberController {
    @GetMapping("/member")
    public String memberHome() {
        log.info("========> '/member', member/index.jsp");
        return "member/index";
    }
}
