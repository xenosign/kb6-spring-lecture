package org.example.kb6spring.controller.post.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.post.PostDto;
import org.example.kb6spring.service.post.PostService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/post/v2")
public class RestPostController {
    private final PostService postService;

    @GetMapping("/list")
    public List<PostDto> list(HttpServletRequest request, Model model) {
        log.info("==========> 게시글 목록 데이터 호출, {}", request.getRequestURI());

        return postService.findAll();
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") int id) {
        log.info("==========> 삭제 기능 호출", "/post/v1/delete");

        int affectedRows = postService.delete(id);

        if (affectedRows == 0) {
            log.error("삭제 실패");
        }
        return "redirect:/post/v1/list";
    }

    @GetMapping("/new")
    public String addPostPage(Model model) {
        log.info("==========> 게시글 추가 페이지 호출", "/post/v1/new");

        return "post/add";
    }

    @PostMapping("/new")
    public String addPost(@RequestParam("title") String title,
                          @RequestParam("content") String content) {
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
