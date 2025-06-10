package org.example.kb6spring.controller.member;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class MemberHomeController {
    @GetMapping("/member/v1")
    public String memberHome() {
        log.info("========> '/member', member/index.jsp");
        return "member/index";
    }
}
