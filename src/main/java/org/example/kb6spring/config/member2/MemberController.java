//package org.example.kb6spring.controller.member;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.kb6spring.dto.member.MemberDto;
//import org.example.kb6spring.service.member.MemberServiceV1;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("/member")
//@Slf4j
//public class MemberController {
//    private final MemberServiceV1 memberService;
//
//    public MemberController(MemberServiceV1 memberService) {
//        this.memberService = memberService;
//    }
//
//    @GetMapping("")
//    public String memberHome() {
//        log.info("========> '/member', member/index.jsp");
//        return "member/index";
//    }
//
//    @GetMapping("/list")
//    public String memberList(Model model) {
//        List<MemberDto> memberList = memberService.getMemberList();
//        model.addAttribute("memberList", memberList);
//        return "member/list";
//    }
//}
