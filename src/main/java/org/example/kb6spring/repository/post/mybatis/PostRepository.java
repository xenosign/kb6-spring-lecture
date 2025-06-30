package org.example.kb6spring.repository.post.mybatis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.post.PostDto;
import org.example.kb6spring.mapper.PostMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostRepository {
    private final PostMapper postMapper;
    private final RedisTemplate<String, Object> redisTemplate;

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


    public void deleteAll() {
        postMapper.deleteAll();
        // Redis 전체 삭제
        redisTemplate.delete(redisTemplate.keys("post:*"));
    }

    public PostDto findById(int id) {
        return postMapper.findById(id);
    }

    public PostDto findByIdFromRedis(int id) {
        Object obj = redisTemplate.opsForValue().get("post:" + id);
        if (obj instanceof PostDto) {
            return (PostDto) obj;
        }
        return null;
    }

    public void save(PostDto post) {
        postMapper.saveForTest(post);
        redisTemplate.opsForValue().set("post:" + post.getId(), post);
    }
}
