package org.example.kb6spring.controller.member.v1;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.service.member.MemberServiceV1;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@Slf4j
public class MemberAddControllerV1 {
    private final MemberServiceV1 memberService;

    public MemberAddControllerV1(MemberServiceV1 memberService) {
        this.memberService = memberService;
    }

//    @PostMapping("/member/add")
//    public String addMember(HttpServletRequest request) {
//        String name = request.getParameter("name");
//        String email = request.getParameter("email");
//
//        log.info("회원 추가 요청: name={}, email={}", name, email);
//        memberService.addMember(name, email);
//
//        return "redirect:/member/list";
//    }

    @PostMapping("/member/add")
    public String addMember(@RequestParam("name") String name,
                            @RequestParam("email") String email) {

        log.info("회원 추가 요청: name={}, email={}", name, email);
        memberService.addMember(name, email);

        return "redirect:/member/list";
    }

    @GetMapping("/member/add")
    public String addMemberPage() {
        return "member/add";
    }
}
