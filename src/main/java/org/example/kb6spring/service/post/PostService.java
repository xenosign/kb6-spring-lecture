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
}
