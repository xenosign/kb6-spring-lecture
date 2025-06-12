package org.example.kb6spring.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.kb6spring.domain.member.MemberEntity;

import java.util.List;

@Mapper
public interface MemberMapper {
    List<MemberEntity> findAll();
    List<MemberEntity> findTwo();
}
