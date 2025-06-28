package org.example.kb6spring.dto.index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexTestDto {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
}