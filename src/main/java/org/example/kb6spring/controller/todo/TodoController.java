package org.example.kb6spring.controller.todo;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.domain.todo.Todo;
import org.example.kb6spring.service.todo.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "Todo 컨트롤러")
@RestController
@Slf4j
@RequiredArgsConstructor
@CrossOrigin("*")
@RequestMapping("/todo")
public class TodoController {
    private final TodoService todoService;

    @GetMapping
    public ResponseEntity<List<Todo>> getAllTodos() {
        List<Todo> todos = todoService.findAll();
        return ResponseEntity.ok(todos);
    }

    @PostMapping
    public ResponseEntity<String> createTodo(@RequestBody Todo todo) {
        todoService.save(todo);
        return ResponseEntity.ok("생성 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Integer id) {
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodo(@PathVariable Integer id) {
        Todo todo = todoService.findById(id);
        if (todo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(todo);
    }

    @PostMapping("/{id}")
    public ResponseEntity<String> updateDone(@PathVariable Integer id) {
        todoService.updateDone(id);

        return ResponseEntity.ok().build();
    }
}
