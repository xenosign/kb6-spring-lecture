package org.example.kb6spring.controller.member.v1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class MemberHomeController {
    @GetMapping("/member")
    public String memberHome() {
        log.info("========> '/member', member/index.jsp");
        return "member/index";
    }
}
