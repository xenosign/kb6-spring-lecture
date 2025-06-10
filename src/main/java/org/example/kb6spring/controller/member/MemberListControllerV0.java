package org.example.kb6spring.controller.member;

import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.member.MemberDto;
import org.example.kb6spring.service.member.MemberServiceV0;
import org.example.kb6spring.service.member.MemberServiceV1;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@Slf4j
public class MemberListControllerV0 {
    // 스프링에 의한 의존성 주입이 아닌 직접 구현한 싱글톤의 인스턴스를 받아서 사용
    private final MemberServiceV0 memberService = MemberServiceV0.getInstance();

    @GetMapping("/member/list-v0")
    public String memberList(Model model) {
        List<MemberDto> memberList = memberService.getMemberList();
        model.addAttribute("memberList", memberList);
        return "member/list";
    }
}
