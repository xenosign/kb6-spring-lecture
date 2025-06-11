package org.example.kb6spring.repository.member;

import org.example.kb6spring.domain.member.MemberEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MemberRepositoryV1 {
    private final List<MemberEntity> memberList = new ArrayList<>();
    public MemberRepositoryV1() {
        memberList.add(new MemberEntity(1L, "ronaldo@example.com", "호날두", "플래티넘", 300000000L));
        memberList.add(new MemberEntity(2L, "sjk@example.com", "송중기", "플래티넘", 300000L));
        memberList.add(new MemberEntity(3L, "xenosign@example.com", "이효석", "아이언", 10L));
    }
    public List<MemberEntity> getMemberList() {
        return memberList;
    }
    public void save(MemberEntity member) {
        member.setId((long) (memberList.size() + 1));
        memberList.add(member);
    }
}
