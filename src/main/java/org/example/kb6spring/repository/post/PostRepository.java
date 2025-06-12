package org.example.kb6spring.repository.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.example.kb6spring.dto.post.PostDto;
import org.example.kb6spring.mapper.PostMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostRepository {
    private final PostMapper postMapper;

    public List<PostDto> findAll() {
        return postMapper.findAll();
    }

    public void save(String title, String content) {
        postMapper.save(title, content);
    }

    public int delete(int id) {
        return postMapper.delete(id);
    }

    public List<PostDto> findByCond(String title, String content) {
        return postMapper.findByCond(title, content);
    }
}
