package org.example.kb6spring.service.member;

import org.example.kb6spring.domain.member.MemberEntity;
import org.example.kb6spring.dto.member.MemberDto;
import org.example.kb6spring.repository.member.MemberRepositoryV0;
import org.example.kb6spring.repository.member.MemberRepositoryV1;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberServiceV0 {
    private static MemberServiceV0 instance;
    private final MemberRepositoryV0 memberRepository;

    // MemberServiceV0 생성 시점에 MemberRepositoryV0 의 인스턴스를 받아서 사용
    private MemberServiceV0() {
        this.memberRepository = MemberRepositoryV0.getInstance();
    }

    // MemberServiceV0 도 싱글톤을 유지해야 하므로 MemberRepositoryV0 과 마찬가지로
    // getInstance 메서드 구현
    public static MemberServiceV0 getInstance() {
        if (instance == null) {
            instance = new MemberServiceV0();
        }
        return instance;
    }

    public List<MemberDto> getMemberList() {
        List<MemberEntity> entityList = memberRepository.getMemberList();
        List<MemberDto> dtoList = new ArrayList<>();

        for (MemberEntity entity : entityList) {
            MemberDto dto = new MemberDto();
            dto.setEmail(entity.getEmail());
            dto.setName(entity.getName());
            dtoList.add(dto);
        }

        return dtoList;
    }
}
