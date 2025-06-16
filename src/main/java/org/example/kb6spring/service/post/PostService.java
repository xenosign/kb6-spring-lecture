package org.example.kb6spring.service.post;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.dto.post.PostDto;
import org.example.kb6spring.repository.post.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public List<PostDto> findAll() {
        return postRepository.findAll();
    }
    public int delete(int id) {
        return postRepository.delete(id);
    }
    public void save(String title, String content) {
        postRepository.save(title, content);
    }
    public List<PostDto> findByCond(String title, String content) {
        return postRepository.findByCond(title, content);
    }

    // DB Compare
    public void resetAndGeneratePosts(int count) {
        postRepository.deleteAll();
        for (int i = 1; i <= count; i++) {
            String title = "제목 " + i;
            String content = "내용 " + i;

            PostDto postDto = new PostDto();
            postDto.setTitle(title);
            postDto.setContent(content);

            postRepository.save(postDto);
        }
    }

    public long testMysqlReadTime(int count) {
        long start = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            postRepository.findById(i);
        }
        return System.currentTimeMillis() - start;
    }

    public long testRedisReadTime(int count) {
        long start = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            postRepository.findByIdFromRedis(i);
        }
        return System.currentTimeMillis() - start;
    }
}
