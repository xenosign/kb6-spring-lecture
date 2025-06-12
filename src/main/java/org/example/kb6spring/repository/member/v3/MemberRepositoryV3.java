package org.example.kb6spring.repository.member.v3;

import org.example.kb6spring.domain.member.MemberEntity;
import org.example.kb6spring.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MemberRepositoryV3 {
    private final MemberMapper memberMapper;

    @Autowired
    public MemberRepositoryV3(final MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public List<MemberEntity> findAll() {
        return memberMapper.findAll();
    }

    public void save(MemberEntity member) {};

    public List<MemberEntity> findTwo() {
        return memberMapper.findTwo();
    }
}
