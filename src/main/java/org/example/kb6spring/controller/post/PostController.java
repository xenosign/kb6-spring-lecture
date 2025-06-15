package org.example.kb6spring.controller.post;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.service.post.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/post/v1")
public class PostController {
    private final PostService postService;

    @GetMapping("/list")
    public String list(Model model) {
        log.info("==========> 게시글 목록 페이지 호출", "/post/v1/list");   

        model.addAttribute("postList", postService.findAll());
        return "post/list";
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
    public String addPost(@RequestParam("title") String title, @RequestParam("content") String content) {
        log.info("==========> 게시글 추가 기능 호출", "/post/v1/new");

        postService.save(title, content);

        return "redirect:/post/v1/list";
    }

    // 게시글 검색
    @GetMapping("/search")
    public String postSearch(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpServletRequest request,
            Model model
    ) {
        log.info("================> 게시글 검색 기능 호출, " + "/post/v1/search");

        model.addAttribute("postList", postService.findByCond(title, content));
        return "post/list";
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
