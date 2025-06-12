package org.example.kb6spring.repository.member.v1;

import org.example.kb6spring.domain.member.MemberEntity;

import java.util.ArrayList;
import java.util.List;

public class MemberRepositoryV0 {
    // 싱글톤 인스턴스 저장용 필드, static 으로 서버 전체에서 공용으로 사용 
    private static MemberRepositoryV0 instance;

    // 생성자를 private으로 만들어 외부에서 클래스를 직접 생성해서 사용하는 것 자체를 방지
    private MemberRepositoryV0() {}

    // 인스턴스가 생성이 안된 경우 생성해서 리턴하고, 아닌 경우 static 에 보관 된 인스턴스의 주소 리턴
    // 이를 통해 서버 전체에서 하나의 인스턴스만 사용이 가능
    public static MemberRepositoryV0 getInstance() {
        if (instance == null) {
            instance = new MemberRepositoryV0();
        }
        return instance;
    }

    public List<MemberEntity> getMemberList() {
        List<MemberEntity> memberList = new ArrayList<>();

        memberList.add(new MemberEntity(1L, "ronaldo@example.com", "호날두", "플래티넘", 300000000L));
        memberList.add(new MemberEntity(2L, "sjk@example.com", "송중기", "플래티넘", 300000L));
        memberList.add(new MemberEntity(3L, "xenosign@example.com", "이효석", "아이언", 10L));

        return memberList;
    }
}
