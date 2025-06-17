package org.example.kb6spring.controller.post.v3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.post.PostDto;
import org.example.kb6spring.service.post.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/post/v3")
public class RestPostControllerV3 {
    private final PostService postService;

    @GetMapping("/list")
    public ResponseEntity<List<PostDto>> list(HttpServletRequest request) {
        log.info("==========> 게시글 목록 데이터 호출, {}", request.getRequestURI());

        List<PostDto> list = postService.findAll();

        return ResponseEntity.ok(list);
    }
    
    @GetMapping(value = "/test", produces="text/plain;charset=UTF-8")
    public ResponseEntity<String> test() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("요청을 처리할 수 없습니다");
    }

    @GetMapping(value = "/test2", produces="text/plain;charset=UTF-8")
    public ResponseEntity<String> test2() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("요청에 대한 응답을 찾을 수 없습니다");
    }

    @PostMapping(value = "/delete", produces="text/plain;charset=UTF-8")
    public ResponseEntity<String> delete(@RequestParam("id") int id, HttpServletRequest request) {
        log.info("==========> 삭제 기능 호출, {}", request.getRequestURI());

        try {
            int result = postService.delete(id);

            if (result == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제 대상을 찾을 수 없습니다");
            }
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류");
        }
    }

    @GetMapping("/new")
    public String addPostPage(Model model) {
        log.info("==========> 게시글 추가 페이지 호출", "/post/v1/new");

        return "post/add";
    }

    @PostMapping("/new")
    public String addPost(@RequestParam("title") String title, @RequestParam("content") String content) {
        log.info("==========> 게시글 추가 기능 호출", "/post/v1/new");

        postService.save(title, content);

        return "redirect:/post/v1/list";
    }

    // 게시글 검색
    @GetMapping("/search")
    public List<PostDto> postSearch(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpServletRequest request,
            Model model
    ) {
        log.info("================> 게시글 검색 기능 호출, {}", request.getRequestURI());

        return postService.findByCond(title, content);
    }

    @GetMapping("/compare")
    public String compare(Model model) {
        log.info("================> DB 비교 기능 호출, " + "/post/v1/compare");

        int count = 1000;
        postService.resetAndGeneratePosts(count);

        long mysqlTime = postService.testMysqlReadTime(count);
        long redisTime = postService.testRedisReadTime(count);

        model.addAttribute("count", count);
        model.addAttribute("mysqlTime", mysqlTime);
        model.addAttribute("redisTime", redisTime);

        return "post/compare";
    }
}
