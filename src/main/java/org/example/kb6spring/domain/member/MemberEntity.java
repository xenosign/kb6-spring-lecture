package org.example.kb6spring.domain.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberEntity {
    private Long id;
    private String email;
    private String name;
    private String grade;
    private Long asset;
}
