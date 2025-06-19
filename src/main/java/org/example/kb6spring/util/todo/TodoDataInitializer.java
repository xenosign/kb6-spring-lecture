package org.example.kb6spring.util.todo;

import lombok.RequiredArgsConstructor;
import org.example.kb6spring.domain.todo.Todo;
import org.example.kb6spring.repository.todo.TodoRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class TodoDataInitializer {
    private final TodoRepository todoRepository;

    @PostConstruct
    public void init() {
        if (todoRepository.findAll().isEmpty()) {
            todoRepository.save(new Todo(null, "JPA 정복", false));
            todoRepository.save(new Todo(null, "회식 하기", true));
            todoRepository.save(new Todo(null, "술 참기", false));
        }
    }
}
